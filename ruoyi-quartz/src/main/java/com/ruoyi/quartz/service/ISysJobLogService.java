package com.ruoyi.quartz.service;

import java.util.List;
import com.ruoyi.quartz.domain.SysJobLog;

/**
 * 定时任务调度日志服务接口
 *
 * <p>在项目中的角色：定义 sys_job_log 表的增删改查契约，
 * 由 {@link com.ruoyi.quartz.service.impl.SysJobLogServiceImpl} 实现。</p>
 *
 * <p>数据流向：日志的唯一写入方是 {@link AbstractQuartzJob#after} 钩子，
 * 读取方是 {@link com.ruoyi.quartz.controller.SysJobLogController}。</p>
 *
 * @author ruoyi
 */
public interface ISysJobLogService
{
    /**
     * 分页查询调度日志列表。
     *
     * @param jobLog 查询条件
     * @return 日志集合
     */
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog);

    /**
     * 根据日志 ID 查询单条日志详情。
     *
     * @param jobLogId 日志主键
     * @return 日志实体
     */
    public SysJobLog selectJobLogById(Long jobLogId);

    /**
     * 新增一条调度日志记录。
     *
     * <p>通常由 Quartz 任务执行完毕后的 {@link AbstractQuartzJob#after} 钩子调用。</p>
     *
     * @param jobLog 日志实体（包含耗时、状态、异常信息等）
     */
    public void addJobLog(SysJobLog jobLog);

    /**
     * 批量删除日志记录。
     *
     * @param logIds 待删除的日志主键数组
     * @return 影响行数
     */
    public int deleteJobLogByIds(Long[] logIds);

    /**
     * 删除单条日志。
     *
     * @param jobId 日志主键
     * @return 影响行数
     */
    public int deleteJobLogById(Long jobId);

    /**
     * 物理清空 sys_job_log 表所有记录。
     */
    public void cleanJobLog();
}
