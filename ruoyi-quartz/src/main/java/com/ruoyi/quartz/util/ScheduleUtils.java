package com.ruoyi.quartz.util;

import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.ScheduleConstants;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.common.exception.job.TaskException.Code;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.quartz.domain.SysJob;

/**
 * 定时任务调度工具类
 *
 * <p>在项目中的角色：Quartz 调度器的核心编排工具，负责将数据库中的 {@link SysJob} 配置
 * 转化为 Quartz 运行时的 {@link JobDetail} + {@link CronTrigger} 组合，并提供安全校验。</p>
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li><b>Cron 表达式解析</b>：委托 {@link CronUtils} 与 Quartz 原生 {@code CronScheduleBuilder}，
 *       先在 Controller 层做合法性检查，运行时直接构建 {@code CronTrigger}。</li>
 *   <li><b>任务防并发机制</b>：根据 {@code SysJob.concurrent} 字段动态选择 Job 实现类——
 *       0（允许并发）使用 {@link QuartzJobExecution}，1（禁止并发）使用带
 *       {@code @DisallowConcurrentExecution} 注解的 {@link QuartzDisallowConcurrentExecution}，
 *       利用 Quartz 自身的 JobDataMap 锁机制保证同一 JobKey 不会重入。</li>
 *   <li><b>Misfire 补偿策略</b>：通过 {@link #handleCronScheduleMisfirePolicy} 封装
 *       {@code MISFIRE_DEFAULT / IGNORE_MISFIRES / FIRE_AND_PROCEED / DO_NOTHING} 四种策略。</li>
 *   <li><b>安全白名单机制</b>：{@link #whiteList} 是 RuoYi 定时任务最关键的安全防线——
 *       对 invokeTarget 字符串做两条路径校验：
 *       <ol>
 *         <li>若 invokeTarget 是全限定类名（含 2 个以上 '.'），直接检查是否以
 *             {@code Constants.JOB_WHITELIST_STR}（默认 {@code com.ruoyi.}）开头；</li>
 *         <li>若 invokeTarget 是 Spring Bean 名称（形如 {@code ryTask.method}），
 *             则通过 {@code SpringUtils.getBean} 拿到 Bean 对象后，检查其包名是否在白名单中，
 *             且不在黑名单 {@code Constants.JOB_ERROR_STR}（{@code java.lang. / java.util.} 等）中。</li>
 *       </ol>
 *       配合 Controller 层的 rmi/ldap/http 关键字拦截，共同防止通过 invokeTarget 注入
 *       InitialContext、SnakeYAML、Spring 容器等高危调用。</li>
 *   <li><b>幂等创建</b>：{@link #createScheduleJob} 在创建前先 {@code scheduler.checkExists}，
 *       若存在则先删再建，避免数据库手动修改导致 Quartz 内部状态与 DB 不一致。</li>
 * </ul>
 *
 * @author ruoyi
 */
public class ScheduleUtils
{
    /**
     * 根据并发标识选择具体的 Quartz Job 实现类。
     *
     * <p>工作原理：当 {@code sysJob.concurrent="0"} 时使用普通的 {@link QuartzJobExecution}，
     * Quartz 允许同一 JobKey 的多个实例并行触发；当 {@code "1"} 时使用
     * {@link QuartzDisallowConcurrentExecution}，该类被 {@code @DisallowConcurrentExecution}
     * 注解修饰，Quartz 会在 JobDataMap 上加锁，同一 JobKey 的前一次执行未结束时，
     * 下一次触发会被阻塞，从而达到"禁止并发"的效果。</p>
     *
     * @param sysJob 任务配置实体，其 {@code concurrent} 字段决定返回哪个 Job 类
     * @return 具体的 Quartz Job 实现类（二选一）
     */
    private static Class<? extends Job> getQuartzJobClass(SysJob sysJob)
    {
        boolean isConcurrent = "0".equals(sysJob.getConcurrent());
        return isConcurrent ? QuartzJobExecution.class : QuartzDisallowConcurrentExecution.class;
    }

    /**
     * 构建 Quartz {@link TriggerKey}。
     *
     * <p>命名规则：{@code ScheduleConstants.TASK_CLASS_NAME + jobId} 作为 key，
     * jobGroup 作为 group，保证在 Scheduler 内唯一标识一个触发器。</p>
     *
     * @param jobId   任务 ID（来自 sys_job 主键）
     * @param jobGroup 任务组名（用于分类管理，如 DEFAULT、SYSTEM）
     * @return Quartz 触发器键对象
     */
    public static TriggerKey getTriggerKey(Long jobId, String jobGroup)
    {
        return TriggerKey.triggerKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 构建 Quartz {@link JobKey}。
     *
     * <p>与 {@link #getTriggerKey} 使用相同的 key/group 规则，
     * 保证 JobDetail 与 Trigger 在 Scheduler 中成对匹配。</p>
     *
     * @param jobId    任务 ID（来自 sys_job 主键）
     * @param jobGroup 任务组名
     * @return Quartz Job 键对象
     */
    public static JobKey getJobKey(Long jobId, String jobGroup)
    {
        return JobKey.jobKey(ScheduleConstants.TASK_CLASS_NAME + jobId, jobGroup);
    }

    /**
     * 在 Quartz Scheduler 中创建（或重建）定时任务。
     *
     * <p>完整流程：</p>
     * <ol>
     *   <li>根据 {@code concurrent} 字段选择 Job 实现类（防并发机制的入口）；</li>
     *   <li>用选定的 Job 类和 {@link #getJobKey} 构建 {@link JobDetail}，
     *       将完整的 {@link SysJob} 配置对象放入 {@code JobDataMap}，
     *       运行时由 {@link AbstractQuartzJob} 通过 {@code getMergedJobDataMap()} 取回；</li>
     *   <li>基于 {@code cronExpression} 构建 {@link CronScheduleBuilder}，
     *       并通过 {@link #handleCronScheduleMisfirePolicy} 应用 Misfire 补偿策略；</li>
     *   <li>用 {@link TriggerBuilder} 生成 {@link CronTrigger}；</li>
     *   <li>幂等保护：若同名 Job 已存在则先删除再重建，
     *       避免手动改数据库后 Quartz 内部残留旧状态；</li>
     *   <li>过期检测：若 Cron 表达式 {@link CronUtils#getNextExecution} 返回 null
     *       （说明表达式已不再产生未来触发点），则跳过调度注册；</li>
     *   <li>初始状态：若 {@code status=PAUSE}，注册后立即调用
     *       {@code scheduler.pauseJob} 将任务置为暂停。</li>
     * </ol>
     *
     * @param scheduler Quartz Scheduler 实例
     * @param job       数据库中的任务配置
     * @throws SchedulerException Quartz 调度器层面异常（如 Job 已存在、Scheduler 未启动等）
     * @throws TaskException      任务配置异常（如 Misfire 策略非法）
     */
    public static void createScheduleJob(Scheduler scheduler, SysJob job) throws SchedulerException, TaskException
    {
        Class<? extends Job> jobClass = getQuartzJobClass(job);
        // 构建job信息
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        JobDetail jobDetail = JobBuilder.newJob(jobClass).withIdentity(getJobKey(jobId, jobGroup)).build();

        // 表达式调度构建器
        CronScheduleBuilder cronScheduleBuilder = CronScheduleBuilder.cronSchedule(job.getCronExpression());
        cronScheduleBuilder = handleCronScheduleMisfirePolicy(job, cronScheduleBuilder);

        // 按新的cronExpression表达式构建一个新的trigger
        CronTrigger trigger = TriggerBuilder.newTrigger().withIdentity(getTriggerKey(jobId, jobGroup))
                .withSchedule(cronScheduleBuilder).build();

        // 放入参数，运行时的方法可以获取
        jobDetail.getJobDataMap().put(ScheduleConstants.TASK_PROPERTIES, job);

        // 判断是否存在
        if (scheduler.checkExists(getJobKey(jobId, jobGroup)))
        {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(getJobKey(jobId, jobGroup));
        }

        // 判断任务是否过期
        if (StringUtils.isNotNull(CronUtils.getNextExecution(job.getCronExpression())))
        {
            // 执行调度任务
            scheduler.scheduleJob(jobDetail, trigger);
        }

        // 暂停任务
        if (job.getStatus().equals(ScheduleConstants.Status.PAUSE.getValue()))
        {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
    }

    /**
     * 为 CronScheduleBuilder 应用 Misfire 补偿策略。
     *
     * <p>当系统宕机或线程池繁忙导致 Cron 触发点被"错过"（misfire）时，
     * Quartz 需要知道应该如何处理这些被错过的触发：</p>
     * <ul>
     *   <li>{@code MISFIRE_DEFAULT} — 保持 Quartz 默认行为（通常是忽略）；</li>
     *   <li>{@code MISFIRE_IGNORE_MISFIRES} — 忽略所有错过的触发，只从当前时刻开始重新调度；</li>
     *   <li>{@code MISFIRE_FIRE_AND_PROCEED} — 立即补触发一次，然后按正常节奏继续；</li>
     *   <li>{@code MISFIRE_DO_NOTHING} — 什么都不做，彻底丢弃错过的触发。</li>
     * </ul>
     *
     * @param job 任务配置，其 {@code misfirePolicy} 字段决定使用哪种补偿策略
     * @param cb  已基于 Cron 表达式构建好的 ScheduleBuilder
     * @return 应用完 Misfire 策略后的 CronScheduleBuilder
     * @throws TaskException 当 {@code misfirePolicy} 取值不在以上四种合法值中时抛出
     */
    public static CronScheduleBuilder handleCronScheduleMisfirePolicy(SysJob job, CronScheduleBuilder cb)
            throws TaskException
    {
        switch (job.getMisfirePolicy())
        {
            case ScheduleConstants.MISFIRE_DEFAULT:
                return cb;
            case ScheduleConstants.MISFIRE_IGNORE_MISFIRES:
                return cb.withMisfireHandlingInstructionIgnoreMisfires();
            case ScheduleConstants.MISFIRE_FIRE_AND_PROCEED:
                return cb.withMisfireHandlingInstructionFireAndProceed();
            case ScheduleConstants.MISFIRE_DO_NOTHING:
                return cb.withMisfireHandlingInstructionDoNothing();
            default:
                throw new TaskException("The task misfire policy '" + job.getMisfirePolicy()
                        + "' cannot be used in cron schedule tasks", Code.CONFIG_ERROR);
        }
    }

    /**
     * 校验 invokeTarget 是否在安全白名单中——定时任务的核心安全边界。
     *
     * <p>问题背景：定时任务允许用户填写任意字符串作为调用目标，若不做约束，
     * 攻击者可构造 {@code java.lang.Runtime.getRuntime.exec("rm -rf /")} 或
     * {@code javax.naming.InitialContext.lookup("ldap://...")} 等高危调用。</p>
     *
     * <p>本方法采用"两段式"校验策略：</p>
     * <ol>
     *   <li>若 invokeTarget 含 2 个以上 "."，说明它是"全限定类名"写法
     *       （如 {@code com.ruoyi.quartz.task.RyTask.ryParams}），
     *       直接检查字符串前缀是否以 {@code Constants.JOB_WHITELIST_STR}（默认 {@code com.ruoyi.}）开头；</li>
     *   <li>若 invokeTarget 只含 1 个 "."，说明它是 Spring Bean 名称写法
     *       （如 {@code ryTask.ryParams}），
     *       先通过 {@code SpringUtils.getBean(beanName)} 拿到 Bean 实例，
     *       再反查其真实包名，检查该包名是否同时满足：
     *       <ul>
     *         <li>以白名单前缀 {@code JOB_WHITELIST_STR} 开头；</li>
     *         <li><b>不以</b>黑名单前缀 {@code JOB_ERROR_STR}（如 {@code java.lang.}、{@code org.springframework.}）开头。</li>
     *       </ul>
     *       这样即使 Bean 名称合法，只要其实现类落在 JDK 或 Spring 核心包下也会被拒绝。</li>
     * </ol>
     *
     * <p>本方法与 Controller 层的 rmi/ldap/http 关键字拦截、
     * {@link JobInvokeUtil} 的反射调用共同构成定时任务的安全三板斧。</p>
     *
     * @param invokeTarget 调用目标字符串，格式为 {@code beanName.method(params)} 或 {@code 全限定类名.method(params)}
     * @return {@code true} 表示通过白名单校验，可以安全调度；{@code false} 表示被拦截
     */
    public static boolean whiteList(String invokeTarget)
    {
        String packageName = StringUtils.substringBefore(invokeTarget, "(");
        int count = StringUtils.countMatches(packageName, ".");
        if (count > 1)
        {
            return StringUtils.startsWithAny(invokeTarget, Constants.JOB_WHITELIST_STR);
        }
        Object obj = SpringUtils.getBean(StringUtils.split(invokeTarget, ".")[0]);
        String beanPackageName = obj.getClass().getPackage().getName();
        return StringUtils.startsWithAny(beanPackageName, Constants.JOB_WHITELIST_STR)
                && !StringUtils.startsWithAny(beanPackageName, Constants.JOB_ERROR_STR);
    }
}
