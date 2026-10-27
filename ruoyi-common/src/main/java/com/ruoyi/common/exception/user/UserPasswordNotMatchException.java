package com.ruoyi.common.exception.user;

/**
 * 用户密码不正确异常类
 * 
 * 当用户登录时输入的密码与数据库存储密码不匹配时抛出此异常，
 * 错误码 "user.password.not.match"。
 * 
 * @author ruoyi
 */
public class UserPasswordNotMatchException extends UserException
{
    private static final long serialVersionUID = 1L;

    /**
     * 默认构造函数，错误码 "user.password.not.match"
     */
    public UserPasswordNotMatchException()
    {
        super("user.password.not.match", null);
    }
}
