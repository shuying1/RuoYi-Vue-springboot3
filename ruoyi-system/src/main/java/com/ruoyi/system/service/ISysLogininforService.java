package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysLogininfor;

/**
 * 系统登录日志 业务接口
 *
 * 提供登录日志的异步写入、条件分页查询、批量删除、清空（TRUNCATE）等能力。
 * 登录日志由 AsyncLogininforService 在登录成功/失败时异步写入，不影响登录接口响应。
 * 依赖：SysLogininforMapper。
 *
 * @author ruoyi
 */
public interface ISysLogininforService
{
    /**
     * 新增系统登录日志
     * 
     * @param logininfor 访问日志对象
     */
    public void insertLogininfor(SysLogininfor logininfor);

    /**
     * 查询系统登录日志集合
     * 
     * @param logininfor 访问日志对象
     * @return 登录记录集合
     */
    public List<SysLogininfor> selectLogininforList(SysLogininfor logininfor);

    /**
     * 批量删除系统登录日志
     * 
     * @param infoIds 需要删除的登录日志ID
     * @return 结果
     */
    public int deleteLogininforByIds(Long[] infoIds);

    /**
     * 清空系统登录日志
     */
    public void cleanLogininfor();
}
