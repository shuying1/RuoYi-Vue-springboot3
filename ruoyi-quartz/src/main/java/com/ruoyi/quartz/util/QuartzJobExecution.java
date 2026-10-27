package com.ruoyi.quartz.util;

import org.quartz.JobExecutionContext;
import com.ruoyi.quartz.domain.SysJob;

/**
 * 允许并发执行的定时任务处理类
 *
 * <p>在项目中的角色：作为 {@link AbstractQuartzJob} 的"允许并发"子类，
 * 供 {@link ScheduleUtils#getQuartzJobClass} 在
 * {@code SysJob.concurrent="0"} 时选择。</p>
 *
 * <p>工作原理：本类没有任何额外注解，Quartz 默认允许同一个 JobKey
 * 被并发触发。与 {@link QuartzDisallowConcurrentExecution}（带
 * {@code @DisallowConcurrentExecution} 注解）形成对偶。</p>
 *
 * @author ruoyi
 * @see QuartzDisallowConcurrentExecution
 */
public class QuartzJobExecution extends AbstractQuartzJob
{
    /**
     * 执行反射调用——直接委托给 {@link JobInvokeUtil#invokeMethod}。
     *
     * <p>本方法在 {@link AbstractQuartzJob#execute} 中被
     * {@code before} 和 {@code after} 包裹，形成完整的"计时 + 日志 + 业务执行"链路。</p>
     *
     * @param context Quartz 工作执行上下文对象
     * @param sysJob  任务配置，其 {@code invokeTarget} 字段决定反射调用目标
     * @throws Exception 反射执行或业务逻辑抛出的任何异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
