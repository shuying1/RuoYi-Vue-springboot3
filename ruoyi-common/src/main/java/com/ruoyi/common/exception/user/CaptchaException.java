package com.ruoyi.common.exception.user;

/**
 * 验证码校验失败异常类
 *
 * <p>用户登录时输入的图形验证码与 Redis 中存储的值不匹配时抛出此异常，
 * 错误码为 "user.jcaptcha.error"，全局异常处理器返回 "验证码错误" 提示。
 *
 * @author ruoyi
 */
public class CaptchaException extends UserException
{
    private static final long serialVersionUID = 1L;

    public CaptchaException()
    {
        super("user.jcaptcha.error", null);
    }
}
