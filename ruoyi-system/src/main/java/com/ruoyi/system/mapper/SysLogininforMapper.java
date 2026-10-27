package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysLogininfor;

/**
 * 系统登录日志表 sys_logininfor 数据访问层
 *
 * 提供登录日志的写入、查询、批量删除和清空操作。
 * 登录日志通过 @Async 异步写入，不影响登录接口响应性能。
 * SQL 映射文件位于 resources/mapper/system/SysLogininforMapper.xml。
 *
 * @author ruoyi
 */
public interface SysLogininforMapper
{
    /**
     * 新增一条登录日志记录
     * <p>由 AsyncLogininforService 异步调用，在登录成功/失败时记录</p>
     *
     * @param logininfor 登录日志对象（包含登录账号、IP、状态、浏览器等）
     */
    public void insertLogininfor(SysLogininfor logininfor);

    /**
     * 条件分页查询登录日志列表
     * <p>支持按登录账号模糊查询、登录状态筛选、登录时间范围查询</p>
     *
     * @param logininfor 登录日志查询条件
     * @return 符合条件的登录日志集合，无数据返回空列表
     */
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor);

    /**
     * 批量删除登录日志
     *
     * @param infoIds 待删除的登录日志主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空所有登录日志记录
     * <p>执行 TRUNCATE TABLE 操作，不可回滚</p>
     *
     * @return 受影响的行数
     */
    public int cleanLogininfor();
}
