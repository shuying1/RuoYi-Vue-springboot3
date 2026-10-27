package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.system.domain.SysUserOnline;

/**
 * 在线用户 业务接口
 *
 * 提供在线会话的查询与 LoginUser → SysUserOnline 的转换能力。
 * 在线会话数据存储在 Redis 中（key 前缀 "login_tokens:"），
 * SysUserOnlineController 会通过 RedisCache.keys 获取所有 token，再逐条调用本接口查询会话详情。
 * 支持按 IP、用户名、IP+用户名 三种条件匹配当前登录用户的会话。
 * 依赖：无外部 Mapper（纯内存/Redis 数据转换）。
 *
 * @author ruoyi
 */
public interface ISysUserOnlineService
{
    /**
     * 通过登录地址查询信息
     * 
     * @param ipaddr 登录地址
     * @param user 用户信息
     * @return 在线用户信息
     */
    public SysUserOnline selectOnlineByIpaddr(String ipaddr, LoginUser user);

    /**
     * 通过用户名称查询信息
     * 
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    public SysUserOnline selectOnlineByUserName(String userName, LoginUser user);

    /**
     * 通过登录地址/用户名称查询信息
     * 
     * @param ipaddr 登录地址
     * @param userName 用户名称
     * @param user 用户信息
     * @return 在线用户信息
     */
    public SysUserOnline selectOnlineByInfo(String ipaddr, String userName, LoginUser user);

    /**
     * 设置在线用户信息
     * 
     * @param user 用户信息
     * @return 在线用户
     */
    public SysUserOnline loginUserToUserOnline(LoginUser user);
}
