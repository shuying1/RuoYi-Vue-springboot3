package com.ruoyi.common.exception.user;

/**
 * 用户密码重试次数超限异常类
 * 
 * 当用户连续密码错误次数超过系统配置的重试上限时抛出此异常。
 * 传入参数包含允许的最大重试次数和锁定时间（分钟），错误码 "user.password.retry.limit.exceed"。
 * 
 * @author ruoyi
 */
public class UserPasswordRetryLimitExceedException extends UserException
{
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     * 
     * @param retryLimitCount 系统允许的最大重试次数
     * @param lockTime        密码锁定时间（分钟）
     */
    public UserPasswordRetryLimitExceedException(int retryLimitCount, int lockTime)
    {
        super("user.password.retry.limit.exceed", new Object[] { retryLimitCount, lockTime });
    }
}
