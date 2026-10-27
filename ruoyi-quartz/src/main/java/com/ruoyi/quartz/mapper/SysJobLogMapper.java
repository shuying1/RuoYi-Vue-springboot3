package com.ruoyi.quartz.mapper;

import java.util.List;
import com.ruoyi.quartz.domain.SysJobLog;

/**
 * 定时任务调度日志 Mapper 接口
 *
 * <p>在项目中的角色：sys_job_log 表的数据访问层。
 * 唯一写入方是 {@link com.ruoyi.quartz.util.AbstractQuartzJob#after}，
 * 读取方是 {@link com.ruoyi.quartz.controller.SysJobLogController}。</p>
 *
 * @author ruoyi
 */
public interface SysJobLogMapper
{
    /**
     * 分页查询调度日志列表。
     *
     * @param jobLog 查询条件
     * @return 日志集合
     */
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog);

    /**
     * 查询全部调度日志。
     *
     * @return 日志集合
     */
    public List<SysJobLog> selectJobLogAll();

    /**
     * 根据日志 ID 查询单条。
     *
     * @param jobLogId 日志主键
     * @return 日志实体
     */
    public SysJobLog selectJobLogById(Long jobLogId);

    /**
     * 新增一条调度日志——被 {@link AbstractQuartzJob#after} 每次任务执行后调用。
     *
     * @param jobLog 日志实体
     * @return 影响行数
     */
    public int insertJobLog(SysJobLog jobLog);

    /**
     * 批量删除日志。
     *
     * @param logIds 日志主键数组
     * @return 影响行数
     */
    public int deleteJobLogByIds(Long[] logIds);

    /**
     * 根据日志 ID 删除单条。
     *
     * @param jobId 日志主键
     * @return 影响行数
     */
    public int deleteJobLogById(Long jobId);

    /**
     * 物理清空 sys_job_log 表全部数据。
     */
    public void cleanJobLog();
}
