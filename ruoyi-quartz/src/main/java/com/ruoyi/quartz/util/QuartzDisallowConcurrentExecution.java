package com.ruoyi.quartz.util;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import com.ruoyi.quartz.domain.SysJob;

/**
 * 禁止并发执行的定时任务处理类
 *
 * <p>在项目中的角色：作为 {@link AbstractQuartzJob} 的"禁止并发"子类，
 * 供 {@link ScheduleUtils#getQuartzJobClass} 在
 * {@code SysJob.concurrent="1"} 时选择。</p>
 *
 * <p>防并发原理：类上的 {@link DisallowConcurrentExecution} 注解告诉 Quartz——
 * 对于同一个 {@link org.quartz.JobDetail}（即同一个 JobKey），
 * 如果前一次执行尚未结束，本次触发会被阻塞而不是立即执行。
 * Quartz 内部通过在 JobDataMap 上加锁实现这一机制，避免数据库层的并发问题。</p>
 *
 * @author ruoyi
 * @see QuartzJobExecution
 */
@DisallowConcurrentExecution
public class QuartzDisallowConcurrentExecution extends AbstractQuartzJob
{
    /**
     * 执行反射调用——直接委托给 {@link JobInvokeUtil#invokeMethod}。
     *
     * <p>与 {@link QuartzJobExecution#doExecute} 实现完全相同，
     * 差异仅在类级别的 {@code @DisallowConcurrentExecution} 注解。</p>
     *
     * @param context Quartz 工作执行上下文对象
     * @param sysJob  任务配置
     * @throws Exception 反射执行或业务逻辑抛出的任何异常
     */
    @Override
    protected void doExecute(JobExecutionContext context, SysJob sysJob) throws Exception
    {
        JobInvokeUtil.invokeMethod(sysJob);
    }
}
