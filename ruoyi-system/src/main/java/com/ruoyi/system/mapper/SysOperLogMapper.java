package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysOperLog;

/**
 * 操作日志表 sys_oper_log 数据访问层
 *
 * 提供操作日志的写入、查询详情、分页列表、批量删除和清空操作。
 * 操作日志通过 @OperLog 注解配合 OperLogAspect 切面自动采集，
 * 由 AsyncOperLogService 异步写入，不影响接口响应性能。
 * SQL 映射文件位于 resources/mapper/system/SysOperLogMapper.xml。
 *
 * @author ruoyi
 */
public interface SysOperLogMapper
{
    /**
     * 新增一条操作日志记录
     * <p>由 OperLogAspect 切面在接口执行后异步调用</p>
     *
     * @param operLog 操作日志对象（包含请求参数、返回结果、耗时等）
     */
    public void insertOperlog(SysOperLog operLog);

    /**
     * 条件分页查询操作日志列表
     * <p>支持按操作模块模糊查询、操作人员模糊查询、业务类型筛选、状态筛选、操作时间范围</p>
     *
     * @param operLog 操作日志查询条件
     * @return 符合条件的操作日志集合，无数据返回空列表
     */
    public List<SysOperLog> selectOperLogList(SysOperLog operLog);

    /**
     * 批量删除操作日志
     *
     * @param operIds 待删除的操作日志主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteOperLogByIds(Long[] operIds);

    /**
     * 查询操作日志详细
     *
     * @param operId 操作日志主键ID
     * @return 操作日志对象，未匹配到返回 null
     */
    public SysOperLog selectOperLogById(Long operId);

    /**
     * 清空所有操作日志记录
     * <p>执行 TRUNCATE TABLE 操作，不可回滚</p>
     */
    public void cleanOperLog();
}
