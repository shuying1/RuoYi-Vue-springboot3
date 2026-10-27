package com.ruoyi.quartz.util;

import java.util.Date;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.ScheduleConstants;
import com.ruoyi.common.utils.ExceptionUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.quartz.domain.SysJob;
import com.ruoyi.quartz.domain.SysJobLog;
import com.ruoyi.quartz.service.ISysJobLogService;

/**
 * 抽象 Quartz Job 基类——模板方法模式实现
 *
 * <p>在项目中的角色：所有定时任务实际执行逻辑的统一基类，
 * {@link QuartzJobExecution}（允许并发）和
 * {@link QuartzDisallowConcurrentExecution}（禁止并发）都继承此类。</p>
 *
 * <p>工作原理：</p>
 * <ol>
 *   <li>Quartz 触发时调用 {@link #execute}，从 {@code JobExecutionContext} 的
 *       {@code JobDataMap} 中取回 {@link ScheduleConstants#TASK_PROPERTIES} 对应的
 *       {@link SysJob} 配置对象；</li>
 *   <li>采用模板方法模式，按 {@code before → doExecute → after} 顺序执行：
 *       <ul>
 *         <li>{@link #before}：在 {@link ThreadLocal} 中记录开始时间——
 *             这里用 ThreadLocal 是因为 Quartz 线程池可能复用同一线程，
 *             每个 Job 实例不独立持有状态；</li>
 *         <li>{@link #doExecute}：抽象方法，由子类实现具体的反射调用逻辑
 *             （实际就是调 {@link JobInvokeUtil#invokeMethod}）；</li>
 *         <li>{@link #after}：无论成功失败都会执行，计算耗时、组装
 *             {@link SysJobLog}、写入数据库，并清理 ThreadLocal。</li>
 *       </ul></li>
 *   <li>异常处理：doExecute 抛出的任何异常都会被 catch，记录到日志后继续
 *       调用 after（保证日志一定会落库），异常本身不再向外传播给 Quartz。</li>
 * </ol>
 *
 * <p>与安全机制的关系：本类本身不做反射或安全检查，
 * 安全校验全部前置在 {@link SysJobController}（新增/修改时）
 * 和 {@link ScheduleUtils#whiteList}（调度前）中。</p>
 *
 * @author ruoyi
 */
public abstract class AbstractQuartzJob implements Job
{
    private static final Logger log = LoggerFactory.getLogger(AbstractQuartzJob.class);

    /**
     * 线程本地变量，用于在 {@link #before} 记录开始时间、在 {@link #after} 读取并计算耗时。
     *
     * <p>设计意图：Quartz 的 Job 实例可能被线程池复用，
     * 且同一个 JobKey 可能有多个并发执行（取决于 concurrent 设置），
     * 用 static ThreadLocal 而非实例字段，保证每次 execute 的开始时间独立。</p>
     */
    private static ThreadLocal<Date> threadLocal = new ThreadLocal<>();

    /**
     * Quartz 触发入口——模板方法主流程。
     *
     * <p>执行顺序：</p>
     * <ol>
     *   <li>从 {@code context.getMergedJobDataMap()} 取出调度时
     *       {@link ScheduleUtils#createScheduleJob} 塞入的 {@link SysJob} 配置；</li>
     *   <li>调用 {@link #before} 记录开始时间；</li>
     *   <li>调用 {@link #doExecute}（子类实现，通常即反射执行 invokeTarget）；</li>
     *   <li>调用 {@link #after} 写日志，无论 doExecute 是否抛异常都会执行；</li>
     *   <li>捕获 Exception 后同样会进入 after，且日志状态记为 FAIL。</li>
     * </ol>
     *
     * @param context Quartz 执行上下文，包含 JobDataMap、Scheduler 等运行时信息
     */
    @Override
    public void execute(JobExecutionContext context)
    {
        SysJob sysJob = new SysJob();
        BeanUtils.copyBeanProp(sysJob, context.getMergedJobDataMap().get(ScheduleConstants.TASK_PROPERTIES));
        try
        {
            before(context, sysJob);
            if (sysJob != null)
            {
                doExecute(context, sysJob);
            }
            after(context, sysJob, null);
        }
        catch (Exception e)
        {
            log.error("任务执行异常  - ：", e);
            after(context, sysJob, e);
        }
    }

    /**
     * 执行前钩子——记录任务开始时间到 ThreadLocal。
     *
     * <p>目前子类未覆盖此方法，留作扩展点（如 MDC 日志链路追踪、
     * 分布式锁预占等）。</p>
     *
     * @param context Quartz 工作执行上下文对象
     * @param sysJob  本次执行的任务配置
     */
    protected void before(JobExecutionContext context, SysJob sysJob)
    {
        threadLocal.set(new Date());
    }

    /**
     * 执行后钩子——统一组装并写入 {@link SysJobLog}。
     *
     * <p>无论任务成功还是抛出异常都会调用本方法，保证每次执行都有日志落库。</p>
     * <p>执行逻辑：</p>
     * <ol>
     *   <li>从 ThreadLocal 取回开始时间，然后立即 remove 防止线程泄漏；</li>
     *   <li>计算耗时（毫秒）；</li>
     *   <li>组装 {@link SysJobLog} 实体；</li>
     *   <li>根据 {@code e} 是否为 null 设置状态：null = SUCCESS，非 null = FAIL；</li>
     *   <li>通过 Spring 容器获取 {@link ISysJobLogService} 完成持久化——
     *       这里用 {@code SpringUtils.getBean} 而非直接注入，
     *       因为 Job 实例由 Quartz 创建，不是 Spring Bean。</li>
     * </ol>
     *
     * @param context Quartz 工作执行上下文对象
     * @param sysJob  本次执行的任务配置
     * @param e       执行过程中捕获的异常，为 null 表示成功
     */
    protected void after(JobExecutionContext context, SysJob sysJob, Exception e)
    {
        Date startTime = threadLocal.get();
        threadLocal.remove();

        final SysJobLog sysJobLog = new SysJobLog();
        sysJobLog.setJobName(sysJob.getJobName());
        sysJobLog.setJobGroup(sysJob.getJobGroup());
        sysJobLog.setInvokeTarget(sysJob.getInvokeTarget());
        sysJobLog.setStartTime(startTime);
        sysJobLog.setEndTime(new Date());
        long runMs = sysJobLog.getEndTime().getTime() - sysJobLog.getStartTime().getTime();
        sysJobLog.setJobMessage(sysJobLog.getJobName() + " 总共耗时：" + runMs + "毫秒");
        if (e != null)
        {
            sysJobLog.setStatus(Constants.FAIL);
            String errorMsg = StringUtils.substring(ExceptionUtil.getExceptionMessage(e), 0, 2000);
            sysJobLog.setExceptionInfo(errorMsg);
        }
        else
        {
            sysJobLog.setStatus(Constants.SUCCESS);
        }

        // 写入数据库当中
        SpringUtils.getBean(ISysJobLogService.class).addJobLog(sysJobLog);
    }

    /**
     * 实际执行的方法，由子类重载。
     *
     * <p>RuoYi 默认两个子类：</p>
     * <ul>
     *   <li>{@link QuartzJobExecution#doExecute} — 允许并发版本；</li>
     *   <li>{@link QuartzDisallowConcurrentExecution#doExecute} — 禁止并发版本。</li>
     * </ul>
     * <p>两者实现完全相同，都是直接调 {@link JobInvokeUtil#invokeMethod}，
     * 差异仅在 Quartz 层面的 {@code @DisallowConcurrentExecution} 注解。</p>
     *
     * @param context Quartz 工作执行上下文对象
     * @param sysJob  本次执行的任务配置
     * @throws Exception 执行过程中的任何异常（反射、业务逻辑都可能抛出）
     */
    protected abstract void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception;
}
