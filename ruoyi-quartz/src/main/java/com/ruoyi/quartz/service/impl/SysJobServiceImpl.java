package com.ruoyi.quartz.service.impl;

import java.util.List;
import jakarta.annotation.PostConstruct;
import org.quartz.JobDataMap;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.ScheduleConstants;
import com.ruoyi.common.exception.job.TaskException;
import com.ruoyi.quartz.domain.SysJob;
import com.ruoyi.quartz.mapper.SysJobMapper;
import com.ruoyi.quartz.service.ISysJobService;
import com.ruoyi.quartz.util.CronUtils;
import com.ruoyi.quartz.util.ScheduleUtils;

/**
 * 定时任务调度服务实现
 *
 * <p>在项目中的角色：{@link ISysJobService} 的唯一实现。除常规 CRUD 外，
 * 负责 <b>数据库 ↔ Quartz Scheduler 双向同步</b>——
 * 每次 DB 操作成功后必须同步操作 Quartz，且整个过程包裹 {@code @Transactional}。</p>
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li>{@link #init()} — {@code @PostConstruct} 启动钩子：应用启动时先清空 Scheduler
 *       （{@code scheduler.clear()}），再把 sys_job 表中所有记录重新注册为 Quartz Job。
 *       解决"手动改数据库导致 Quartz 内存状态与 DB 不一致"的问题。</li>
 *   <li>{@link #insertJob} — 强制设置 status=PAUSE（防止新任务立即触发），
 *       写库后调 {@link ScheduleUtils#createScheduleJob} 注册到 Quartz。</li>
 *   <li>{@link #updateJob} — 更新 DB 后调用 {@link #updateSchedulerJob} 做"先删后建"
 *       幂等重建，保证 Cron、invokeTarget、concurrent 等字段改动立即生效。</li>
 *   <li>{@link #pauseJob}/{@link #resumeJob}/{@link #deleteJob} — 都是"先 DB 后 Quartz"
 *       顺序，DB 成功才操作 Quartz，事务回滚时 Quartz 操作也会被 Spring 处理（Quartz 的
 *       Scheduler 操作大多是幂等或可重试的）。</li>
 *   <li>{@link #run} — 立即触发用 {@code scheduler.triggerJob(jobKey, jobDataMap)}，
 *       复用已注册 Job 的 {@code JobDetail} 配置，只是绕过 Cron。</li>
 * </ul>
 *
 * @author ruoyi
 */
@Service
public class SysJobServiceImpl implements ISysJobService
{
    @Autowired
    private Scheduler scheduler;

    @Autowired
    private SysJobMapper jobMapper;

    /**
     * 应用启动时的定时任务初始化——从数据库重建整个 Quartz 调度表。
     *
     * <p>先 {@code scheduler.clear()} 清空内存中所有 Job，再逐条从 sys_job 表读取并
     * 通过 {@link ScheduleUtils#createScheduleJob} 重新注册。</p>
     * <p>设计意图：防止运维人员手动修改数据库中的 Cron、invokeTarget 等字段后，
     * Quartz 内存中仍是旧状态。代价是如果有人手动改了数据库的 ID 和 jobGroup
     * （这两个是 Quartz JobKey 的组成部分），会导致脏数据——因此文档中明确警告
     * "不要手动改这两个字段"。</p>
     *
     * @throws SchedulerException Quartz Scheduler 未就绪时抛出
     * @throws TaskException      数据库中存在非法 Cron 或 Misfire 策略时抛出
     */
    @PostConstruct
    public void init() throws SchedulerException, TaskException
    {
        scheduler.clear();
        List<SysJob> jobList = jobMapper.selectJobAll();
        for (SysJob job : jobList)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
    }

    /**
     * 查询定时任务列表。
     *
     * @param job 查询条件
     * @return 任务集合
     */
    @Override
    public List<SysJob> selectJobList(SysJob job)
    {
        return jobMapper.selectJobList(job);
    }

    /**
     * 根据任务 ID 查询。
     *
     * @param jobId 任务主键
     * @return 任务实体
     */
    @Override
    public SysJob selectJobById(Long jobId)
    {
        return jobMapper.selectJobById(jobId);
    }

    /**
     * 暂停任务——DB status → PAUSE + scheduler.pauseJob。
     *
     * @param job 只需 jobId、jobGroup 有效
     * @return 影响行数
     * @throws SchedulerException Quartz 暂停失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int pauseJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = jobMapper.updateJob(job);
        if (rows > 0)
        {
            scheduler.pauseJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 恢复任务——DB status → NORMAL + scheduler.resumeJob。
     *
     * @param job 只需 jobId、jobGroup 有效
     * @return 影响行数
     * @throws SchedulerException Quartz 恢复失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resumeJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        job.setStatus(ScheduleConstants.Status.NORMAL.getValue());
        int rows = jobMapper.updateJob(job);
        if (rows > 0)
        {
            scheduler.resumeJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 删除任务——DB 删除 + scheduler.deleteJob。
     *
     * @param job 只需 jobId、jobGroup 有效
     * @return 影响行数
     * @throws SchedulerException Quartz 删除失败
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteJob(SysJob job) throws SchedulerException
    {
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        int rows = jobMapper.deleteJobById(jobId);
        if (rows > 0)
        {
            scheduler.deleteJob(ScheduleUtils.getJobKey(jobId, jobGroup));
        }
        return rows;
    }

    /**
     * 批量删除——逐条调 {@link #deleteJob}，保证每条都同步操作 DB + Quartz。
     *
     * @param jobIds 任务主键数组
     * @throws SchedulerException Quartz 层面异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteJobByIds(Long[] jobIds) throws SchedulerException
    {
        for (Long jobId : jobIds)
        {
            SysJob job = jobMapper.selectJobById(jobId);
            deleteJob(job);
        }
    }

    /**
     * 任务状态切换——根据传入 status 自动路由到 pause 或 resume。
     *
     * @param job 包含 status 的请求体
     * @return 影响行数
     * @throws SchedulerException Quartz 层面异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int changeStatus(SysJob job) throws SchedulerException
    {
        int rows = 0;
        String status = job.getStatus();
        if (ScheduleConstants.Status.NORMAL.getValue().equals(status))
        {
            rows = resumeJob(job);
        }
        else if (ScheduleConstants.Status.PAUSE.getValue().equals(status))
        {
            rows = pauseJob(job);
        }
        return rows;
    }

    /**
     * 立即手动触发一次任务。
     *
     * <p>实现：从 DB 读取完整任务配置（确保 invokeTarget/Cron 等是最新的），
     * 放入 JobDataMap 后调 {@code scheduler.triggerJob}。
     * 触发前先检查 Quartz 中该 JobKey 是否存在，不存在返回 false。</p>
     *
     * @param job 只需 jobId、jobGroup 有效
     * @return true 表示成功触发
     * @throws SchedulerException Quartz 层面异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean run(SysJob job) throws SchedulerException
    {
        boolean result = false;
        Long jobId = job.getJobId();
        String jobGroup = job.getJobGroup();
        SysJob properties = selectJobById(job.getJobId());
        // 参数
        JobDataMap dataMap = new JobDataMap();
        dataMap.put(ScheduleConstants.TASK_PROPERTIES, properties);
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            result = true;
            scheduler.triggerJob(jobKey, dataMap);
        }
        return result;
    }

    /**
     * 新增定时任务——先写 DB，再注册 Quartz。
     *
     * <p>新增任务强制设为 PAUSE 状态（用户需手动启用），
     * 避免因前端漏传 status 导致任务被立即触发。</p>
     *
     * @param job 完整任务配置
     * @return 影响行数
     * @throws SchedulerException Quartz 注册异常
     * @throws TaskException      配置异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertJob(SysJob job) throws SchedulerException, TaskException
    {
        job.setStatus(ScheduleConstants.Status.PAUSE.getValue());
        int rows = jobMapper.insertJob(job);
        if (rows > 0)
        {
            ScheduleUtils.createScheduleJob(scheduler, job);
        }
        return rows;
    }

    /**
     * 更新任务——先写 DB，再幂等重建 Quartz Job。
     *
     * <p>重建用的是更新后的 job 对象（含新的 Cron、invokeTarget、concurrent 等），
     * 而 jobGroup 仍用旧值——因为 updateSchedulerJob 签名接收的 jobGroup
     * 是为了先按旧 key 找到旧 Job 再删除，避免 Quartz 状态残留。</p>
     *
     * @param job 含 jobId 和所有更新字段
     * @return 影响行数
     * @throws SchedulerException Quartz 重建异常
     * @throws TaskException      配置异常
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateJob(SysJob job) throws SchedulerException, TaskException
    {
        SysJob properties = selectJobById(job.getJobId());
        int rows = jobMapper.updateJob(job);
        if (rows > 0)
        {
            updateSchedulerJob(job, properties.getJobGroup());
        }
        return rows;
    }

    /**
     * Quartz Job 幂等重建——先删后建。
     *
     * <p>Quartz 不支持"就地修改"已注册 Job 的 Cron 或 invokeTarget，
     * 因此修改时必须先按旧 JobKey 删掉旧 Job，再按新配置重新创建。</p>
     *
     * @param job      更新后的任务配置（含新值）
     * @param jobGroup 旧的 jobGroup（用于定位旧 Job）
     * @throws SchedulerException Quartz 操作异常
     * @throws TaskException      配置异常
     */
    public void updateSchedulerJob(SysJob job, String jobGroup) throws SchedulerException, TaskException
    {
        Long jobId = job.getJobId();
        // 判断是否存在
        JobKey jobKey = ScheduleUtils.getJobKey(jobId, jobGroup);
        if (scheduler.checkExists(jobKey))
        {
            // 防止创建时存在数据问题 先移除，然后在执行创建操作
            scheduler.deleteJob(jobKey);
        }
        ScheduleUtils.createScheduleJob(scheduler, job);
    }

    /**
     * 校验 Cron 表达式合法性。
     *
     * @param cronExpression 待校验的 Cron 表达式
     * @return true 表示合法
     */
    @Override
    public boolean checkCronExpressionIsValid(String cronExpression)
    {
        return CronUtils.isValid(cronExpression);
    }
}
