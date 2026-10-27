package com.ruoyi.quartz.mapper;

import java.util.List;
import com.ruoyi.quartz.domain.SysJob;

/**
 * 定时任务调度 Mapper 接口
 *
 * <p>在项目中的角色：sys_job 表的数据访问层，提供定时任务的 CRUD。
 * 与 Quartz Scheduler 的"双向同步"不在这里，而在
 * {@link com.ruoyi.quartz.service.impl.SysJobServiceImpl} 中完成。</p>
 *
 * @author ruoyi
 */
public interface SysJobMapper
{
    /**
     * 分页查询定时任务列表。
     *
     * @param job 查询条件
     * @return 任务集合
     */
    public List<SysJob> selectJobList(SysJob job);

    /**
     * 查询全部定时任务——供 {@link com.ruoyi.quartz.service.impl.SysJobServiceImpl#init()}
     * 在启动时加载所有任务到 Quartz Scheduler。
     *
     * @return 全部任务集合
     */
    public List<SysJob> selectJobAll();

    /**
     * 根据任务 ID 查询单条记录。
     *
     * @param jobId 任务主键
     * @return 任务实体
     */
    public SysJob selectJobById(Long jobId);

    /**
     * 根据任务 ID 删除。
     *
     * @param jobId 任务主键
     * @return 影响行数
     */
    public int deleteJobById(Long jobId);

    /**
     * 批量删除。
     *
     * @param ids 任务主键数组
     * @return 影响行数
     */
    public int deleteJobByIds(Long[] ids);

    /**
     * 更新任务配置（Cron、invokeTarget、concurrent、status 等）。
     *
     * @param job 更新后的任务实体
     * @return 影响行数
     */
    public int updateJob(SysJob job);

    /**
     * 新增一条定时任务配置。
     *
     * @param job 任务实体
     * @return 影响行数
     */
    public int insertJob(SysJob job);
}
