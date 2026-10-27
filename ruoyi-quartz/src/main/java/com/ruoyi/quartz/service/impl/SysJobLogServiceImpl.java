package com.ruoyi.quartz.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.quartz.domain.SysJobLog;
import com.ruoyi.quartz.mapper.SysJobLogMapper;
import com.ruoyi.quartz.service.ISysJobLogService;

/**
 * 定时任务调度日志服务实现
 *
 * <p>在项目中的角色：{@link ISysJobLogService} 的唯一实现，
 * 直接委托给 {@link SysJobLogMapper} 完成数据访问。</p>
 *
 * <p>调用方：唯一写入方是 {@link AbstractQuartzJob#after}
 * （通过 {@code SpringUtils.getBean(ISysJobLogService.class).addJobLog}）；
 * 读取方是 {@link com.ruoyi.quartz.controller.SysJobLogController}。</p>
 *
 * @author ruoyi
 */
@Service
public class SysJobLogServiceImpl implements ISysJobLogService
{
    @Autowired
    private SysJobLogMapper jobLogMapper;

    /**
     * 分页查询调度日志列表。
     *
     * @param jobLog 查询条件
     * @return 日志集合
     */
    @Override
    public List<SysJobLog> selectJobLogList(SysJobLog jobLog)
    {
        return jobLogMapper.selectJobLogList(jobLog);
    }

    /**
     * 根据日志 ID 查询单条日志详情。
     *
     * @param jobLogId 日志主键
     * @return 日志实体
     */
    @Override
    public SysJobLog selectJobLogById(Long jobLogId)
    {
        return jobLogMapper.selectJobLogById(jobLogId);
    }

    /**
     * 新增一条调度日志记录。
     *
     * <p>被 {@link AbstractQuartzJob#after} 在每次定时任务执行后调用。</p>
     *
     * @param jobLog 日志实体
     */
    @Override
    public void addJobLog(SysJobLog jobLog)
    {
        jobLogMapper.insertJobLog(jobLog);
    }

    /**
     * 批量删除调度日志。
     *
     * @param logIds 日志主键数组
     * @return 影响行数
     */
    @Override
    public int deleteJobLogByIds(Long[] logIds)
    {
        return jobLogMapper.deleteJobLogByIds(logIds);
    }

    /**
     * 删除单条日志。
     *
     * @param jobId 日志主键
     * @return 影响行数
     */
    @Override
    public int deleteJobLogById(Long jobId)
    {
        return jobLogMapper.deleteJobLogById(jobId);
    }

    /**
     * 物理清空 sys_job_log 表所有数据。
     */
    @Override
    public void cleanJobLog()
    {
        jobLogMapper.cleanJobLog();
    }
}
