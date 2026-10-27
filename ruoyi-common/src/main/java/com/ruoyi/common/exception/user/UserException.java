package com.ruoyi.common.exception.user;

import com.ruoyi.common.exception.base.BaseException;

/**
 * 用户相关异常基类
 * 
 * 继承 {@link BaseException}，固定所属模块为 "user"。
 * 用户认证、授权、登录等场景的子异常类都应继承此类。
 * 
 * @author ruoyi
 */
public class UserException extends BaseException
{
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     * 
     * @param code 错误码（在资源文件中定义）
     * @param args 消息模板参数
     */
    public UserException(String code, Object[] args)
    {
        super("user", code, args, null);
    }
}
