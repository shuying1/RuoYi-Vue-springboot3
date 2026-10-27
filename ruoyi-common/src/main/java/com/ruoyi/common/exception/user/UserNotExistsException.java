package com.ruoyi.common.exception.user;

/**
 * 用户不存在异常类
 * 
 * 当根据用户名查询用户信息返回空时抛出此异常，错误码 "user.not.exists"。
 * 
 * @author ruoyi
 */
public class UserNotExistsException extends UserException
{
    private static final long serialVersionUID = 1L;

    /**
     * 默认构造函数，错误码 "user.not.exists"
     */
    public UserNotExistsException()
    {
        super("user.not.exists", null);
    }
}
