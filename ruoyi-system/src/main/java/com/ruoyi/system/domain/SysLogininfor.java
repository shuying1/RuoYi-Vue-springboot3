package com.ruoyi.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 系统访问记录表 sys_logininfor
 *
 * 登录日志实体类，对应数据库表 sys_logininfor。
 * 用于记录每次用户登录系统的详细信息，包括登录账号、登录IP、登录地点、浏览器、操作系统、
 * 登录状态（成功/失败）以及提示消息等。
 * 登录日志通过 @Async 异步方式写入，避免影响登录性能。
 * 数据来源：由 AsyncLogininforService 在登录成功/失败时调用 SysLogininforMapper.insertLogininfor 写入。
 *
 * @author ruoyi
 */
public class SysLogininfor extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志主键ID，数据库自增 */
    @Excel(name = "序号", cellType = ColumnType.NUMERIC)
    private Long infoId;

    /** 用户登录账号，对应 sys_user.user_name；不可空 */
    @Excel(name = "用户账号")
    private String userName;

    /** 登录状态：0=成功，1=失败；不可空 */
    @Excel(name = "登录状态", readConverterExp = "0=成功,1=失败")
    private String status;

    /** 登录IP地址，由 SecurityUtils.getLoginUser 获取；不可空 */
    @Excel(name = "登录地址")
    private String ipaddr;

    /** 登录地点，由 IP 地址通过 IP 地址库解析获得，如"广东省深圳市"；可空 */
    @Excel(name = "登录地点")
    private String loginLocation;

    /** 浏览器类型，从 User-Agent 解析，如"Chrome 120"；可空 */
    @Excel(name = "浏览器")
    private String browser;

    /** 操作系统，从 User-Agent 解析，如"Windows 10"；可空 */
    @Excel(name = "操作系统")
    private String os;

    /** 提示消息，成功时为"登录成功"，失败时为具体错误信息（如"用户不存在"、"密码错误"）；可空 */
    @Excel(name = "提示消息")
    private String msg;

    /** 访问时间，即登录发生的时间点；不可空 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date loginTime;

    /**
     * 获取日志主键ID
     *
     * @return 日志记录主键
     */
    public Long getInfoId()
    {
        return infoId;
    }

    /**
     * 设置日志主键ID
     *
     * @param infoId 日志主键
     */
    public void setInfoId(Long infoId)
    {
        this.infoId = infoId;
    }

    /**
     * 获取用户登录账号
     *
     * @return 登录账号字符串
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * 设置用户登录账号
     *
     * @param userName 登录账号
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * 获取登录状态
     *
     * @return 0=成功，1=失败
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * 设置登录状态
     *
     * @param status 登录状态：0=成功，1=失败
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * 获取登录IP地址
     *
     * @return IP 地址字符串，如"127.0.0.1"
     */
    public String getIpaddr()
    {
        return ipaddr;
    }

    /**
     * 设置登录IP地址
     *
     * @param ipaddr 登录IP地址
     */
    public void setIpaddr(String ipaddr)
    {
        this.ipaddr = ipaddr;
    }

    /**
     * 获取登录地点
     *
     * @return 登录地中文描述，如"广东省深圳市"，可空
     */
    public String getLoginLocation()
    {
        return loginLocation;
    }

    /**
     * 设置登录地点
     *
     * @param loginLocation 登录地描述
     */
    public void setLoginLocation(String loginLocation)
    {
        this.loginLocation = loginLocation;
    }

    /**
     * 获取浏览器类型
     *
     * @return 浏览器名称+版本，如"Chrome 120"，可空
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
     * @return 操作系统名称，如"Windows 10"，可空
     */
    public String getOs()
    {
        return os;
    }

    /**
     * 设置操作系统
     *
     * @param os 操作系统名称
     */
    public void setOs(String os)
    {
        this.os = os;
    }

    /**
     * 获取提示消息
     *
     * @return 登录结果提示信息，可空
     */
    public String getMsg()
    {
        return msg;
    }

    /**
     * 设置提示消息
     *
     * @param msg 提示消息文本
     */
    public void setMsg(String msg)
    {
        this.msg = msg;
    }

    /**
     * 获取访问时间
     *
     * @return 登录发生的时间点
     */
    public Date getLoginTime()
    {
        return loginTime;
    }

    /**
     * 设置访问时间
     *
     * @param loginTime 登录时间
     */
    public void setLoginTime(Date loginTime)
    {
        this.loginTime = loginTime;
    }
}
