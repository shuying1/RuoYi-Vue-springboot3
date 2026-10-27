package com.ruoyi.common.exception.user;

/**
 * 黑名单IP异常类
 *
 * <p>当请求端 IP 命中系统配置的登录黑名单时抛出此异常，
 * 错误码为 "login.blocked"，由全局异常处理器转换为 "抱歉，您的IP地址不在允许范围内" 提示。
 * 黑名单通常用于封禁恶意爬取或暴力破解的来源 IP。
 *
 * @author ruoyi
 */
public class BlackListException extends UserException
{
    private static final long serialVersionUID = 1L;

    public BlackListException()
    {
        super("login.blocked", null);
    }
}
