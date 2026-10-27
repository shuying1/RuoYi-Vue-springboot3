package com.ruoyi.common.exception.user;

/**
 * 验证码过期异常类
 *
 * <p>用户登录时图形验证码已超过有效期（默认2分钟，由 {@link com.ruoyi.common.constant.Constants#CAPTCHA_EXPIRATION} 决定）
 * 或已被消费时抛出此异常，错误码为 "user.jcaptcha.expire"，全局异常处理器返回 "验证码已过期，请重新获取" 提示。
 *
 * @author ruoyi
 */
public class CaptchaExpireException extends UserException
{
    private static final long serialVersionUID = 1L;

    public CaptchaExpireException()
    {
        super("user.jcaptcha.expire", null);
    }
}
