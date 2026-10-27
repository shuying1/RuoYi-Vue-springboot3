package com.ruoyi.quartz.service;

import java.util.List;
import org.quartz.SchedulerException;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.quartz.domain.SysJob;

/**
 * 定时任务调度服务接口
 *
 * <p>在项目中的角色：定义 sys_job 表相关的业务操作契约，
 * 由 {@link com.ruoyi.quartz.service.impl.SysJobServiceImpl} 实现。</p>
 *
 * <p>职责：除了常规 CRUD 外，还负责与 Quartz Scheduler 的双向同步——
 * 数据库的状态变更（pause/resume/delete）必须立即反映到 Quartz 中，
 * 因此 Service 层方法都带 {@code @Transactional} 且同时操作 DB 和 Scheduler。</p>
 *
 * @author ruoyi
 */
public interface ISysJobService
{
    /**
     * 分页查询定时任务列表。
     *
     * @param job 查询条件（支持 jobName、jobGroup、status 等模糊/精确匹配）
     * @return 符合条件的任务集合
     */
    public List<SysJob> selectJobList(SysJob job);

    /**
     * 根据任务 ID 查询完整任务配置。
     *
     * @param jobId 任务主键
     * @return 任务实体；若不存在返回 null
     */
    public SysJob selectJobById(Long jobId);

    /**
     * 暂停任务——同时更新数据库状态和调用 {@code scheduler.pauseJob}。
     *
     * @param job 只需要 jobId、jobGroup 字段有效
     * @return 影响行数
     * @throws SchedulerException Quartz 层面暂停失败时抛出
     */
    public int pauseJob(SysJob job) throws SchedulerException;

    /**
     * 恢复任务——同时更新数据库状态和调用 {@code scheduler.resumeJob}。
     *
     * @param job 只需要 jobId、jobGroup 字段有效
     * @return 影响行数
     * @throws SchedulerException Quartz 层面恢复失败时抛出
     */
    public int resumeJob(SysJob job) throws SchedulerException;

    /**
     * 删除任务——数据库删除 + 从 Quartz Scheduler 中移除对应的 Job/Trigger。
     *
     * @param job 只需要 jobId、jobGroup 字段有效
     * @return 影响行数
     * @throws SchedulerException Quartz 层面删除失败时抛出
     */
    public int deleteJob(SysJob job) throws SchedulerException;

    /**
     * 批量删除任务。
     *
     * @param jobIds 待删除的任务主键数组
     * @throws SchedulerException Quartz 层面异常
     */
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException;

    /**
     * 切换任务状态——根据传入的 status 是 NORMAL 还是 PAUSE，
     * 自动委托 {@link #resumeJob} 或 {@link #pauseJob}。
     *
     * @param job 包含 jobId、jobGroup、status 的请求体
     * @return 影响行数
     * @throws SchedulerException Quartz 层面异常
     */
    public int changeStatus(SysJob job) throws SchedulerException;

    /**
     * 立即手动触发一次定时任务（忽略 Cron 表达式）。
     *
     * <p>内部通过 {@code scheduler.triggerJob(jobKey, jobDataMap)} 实现，
     * 触发前会检查 Quartz 中是否已存在该 JobKey，不存在则返回 false。</p>
     *
     * @param job 包含 jobId、jobGroup 的请求体
     * @return true 表示触发成功；false 表示任务不存在或已过期
     * @throws SchedulerException Quartz 层面异常
     */
    public boolean run(SysJob job) throws SchedulerException;

    /**
     * 新增定时任务。
     *
     * <p>实现细节：先将任务状态强制设为 PAUSE（避免用户漏填导致立即触发），
     * 写入数据库后再通过 {@link ScheduleUtils#createScheduleJob} 注册到 Quartz。</p>
     *
     * @param job 任务完整配置
     * @return 影响行数
     * @throws SchedulerException Quartz 注册异常
     * @throws TaskException      Cron 表达式或 Misfire 策略异常
     */
    public int insertJob(SysJob job) throws SchedulerException, TaskException;

    /**
     * 更新定时任务。
     *
     * <p>实现细节：更新数据库后，调用 {@link ScheduleUtils#createScheduleJob}
     * 先删后建 Quartz Job（幂等重建），保证修改后的 Cron、invokeTarget、
     * concurrent 等字段立即生效。</p>
     *
     * @param job 包含 jobId 和所有待更新字段的任务实体
     * @return 影响行数
     * @throws SchedulerException Quartz 注册异常
     * @throws TaskException      Cron 表达式或 Misfire 策略异常
     */
    public int updateJob(SysJob job) throws SchedulerException, TaskException;

    /**
     * 校验给定的 Cron 表达式是否合法。
     *
     * <p>直接委托 {@link com.ruoyi.quartz.util.CronUtils#isValid}。</p>
     *
     * @param cronExpression 待校验的 Cron 表达式
     * @return true 表示合法
     */
    public boolean checkCronExpressionIsValid(String cronExpression);
}
