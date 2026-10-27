package com.ruoyi.system.domain;

/**
 * 当前在线会话
 *
 * 在线用户会话信息封装类，不对应数据库表。
 * 用于在系统监控-在线用户界面展示当前所有在线用户的会话详情。
 * 数据来源于 Redis 缓存中的 LoginUser 对象（key 前缀 "login_tokens:"），
 * 由 SysUserOnlineService 从 Redis 中反序列化并组装本对象返回。
 * tokenId 作为会话的唯一标识，可用于强制下线操作（SecurityUtils.logout）。
 *
 * @author ruoyi
 */
public class SysUserOnline
{
    /** 会话编号，即登录 token（Redis key 中的唯一标识）；不可空 */
    private String tokenId;

    /** 部门名称，当前登录用户所属部门；可空 */
    private String deptName;

    /** 用户登录账号；不可空 */
    private String userName;

    /** 登录 IP 地址；可空 */
    private String ipaddr;

    /** 登录地点，IP 解析得到；可空 */
    private String loginLocation;

    /** 浏览器类型；可空 */
    private String browser;

    /** 操作系统；可空 */
    private String os;

    /** 登录时间（时间戳毫秒数）；不可空 */
    private Long loginTime;

    /**
     * 获取会话编号
     *
     * @return token 字符串
     */
    public String getTokenId()
    {
        return tokenId;
    }

    /**
     * 设置会话编号
     *
     * @param tokenId token 标识
     */
    public void setTokenId(String tokenId)
    {
        this.tokenId = tokenId;
    }

    /**
     * 获取部门名称
     *
     * @return 部门中文名
     */
    public String getDeptName()
    {
        return deptName;
    }

    /**
     * 设置部门名称
     *
     * @param deptName 部门名
     */
    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    /**
     * 获取用户账号
     *
     * @return 登录账号
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * 设置用户账号
     *
     * @param userName 登录账号
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * 获取登录IP
     *
     * @return IP 地址字符串
     */
    public String getIpaddr()
    {
        return ipaddr;
    }

    /**
     * 设置登录IP
     *
     * @param ipaddr IP 地址
     */
    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    /**
     * 获取登录地点
     *
     * @return IP 解析后的地点，可空
     */
    public String getLoginLocation()
    {
        return loginLocation;
    }

    /**
     * 设置登录地点
     *
     * @param loginLocation 地点描述
     */
    public void setLoginLocation(String loginLocation)
    {
        this.loginLocation = loginLocation;
    }

    /**
     * 获取浏览器类型
     *
     * @return 浏览器名称+版本
     */
    public String getBrowser()
    {
        return browser;
    }

    /**
     * 设置浏览器类型
     *
     * @param browser 浏览器类型
     */
    public void setBrowser(String browser)
    {
        this.browser = browser;
    }

    /**
     * 获取操作系统
     *
     * @return 操作系统名称
     */
    public String getOs()
    {
        return os;
    }

    /**
     * 设置操作系统
     *
     * @param os 操作系统
     */
    public void setOs(String os)
    {
        this.os = os;
    }

    /**
     * 获取登录时间戳
     *
     * @return 毫秒数
     */
    public Long getLoginTime()
    {
        return loginTime;
    }

    /**
     * 设置登录时间戳
     *
     * @param loginTime 时间戳（毫秒）
     */
    public void setLoginTime(Long loginTime)
    {
        this.loginTime = loginTime;
    }
}
