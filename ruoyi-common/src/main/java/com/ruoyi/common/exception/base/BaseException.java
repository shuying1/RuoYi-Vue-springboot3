package com.ruoyi.common.exception.base;

import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 基础异常类
 * 
 * 继承 RuntimeException，作为模块级异常的基类。
 * 错误消息支持通过错误码和参数从资源文件（message.properties）中解析国际化消息，
 * 也支持直接传入默认消息。
 *
 * 构造时指定所属模块（如 "file"、"job"），便于日志分类。
 * 子类应构造时调用正确的父构造函数。
 * 
 * @author ruoyi
 */
public class BaseException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 异常所属模块标识，如 "file"、"job"、"user"
     */
    private String module;

    /**
     * 错误码，用于从资源文件中查找对应消息模板
     */
    private String code;

    /**
     * 错误码对应的参数数组，用于替换消息模板中的占位符
     */
    private Object[] args;

    /**
     * 默认错误消息（当code为null或资源文件查找失败时使用）
     */
    private String defaultMessage;

    /**
     * 全参数构造函数
     * 
     * @param module         所属模块标识
     * @param code           错误码
     * @param args           消息模板参数
     * @param defaultMessage 默认消息
     */
    public BaseException(String module, String code, Object[] args, String defaultMessage)
    {
        this.module = module;
        this.code = code;
        this.args = args;
        this.defaultMessage = defaultMessage;
    }

    /**
     * 构造函数，指定模块、错误码和参数，无默认消息
     * 
     * @param module 所属模块标识
     * @param code   错误码
     * @param args   消息模板参数
     */
    public BaseException(String module, String code, Object[] args)
    {
        this(module, code, args, null);
    }

    /**
     * 构造函数，指定模块和默认消息，无错误码
     * 
     * @param module         所属模块标识
     * @param defaultMessage 默认错误消息
     */
    public BaseException(String module, String defaultMessage)
    {
        this(module, null, null, defaultMessage);
    }

    /**
     * 构造函数，指定错误码和参数，无模块标识
     * 
     * @param code 错误码
     * @param args 消息模板参数
     */
    public BaseException(String code, Object[] args)
    {
        this(null, code, args, null);
    }

    /**
     * 最简单的构造函数，仅设置默认消息
     * 
     * @param defaultMessage 错误消息
     */
    public BaseException(String defaultMessage)
    {
        this(null, null, null, defaultMessage);
    }

    /**
     * 获取异常消息
     * 优先通过MessageUtils根据code解析国际化消息，解析失败返回defaultMessage
     * 
     * @return 异常描述消息
     */
    @Override
    public String getMessage()
    {
        String message = null;
        if (!StringUtils.isEmpty(code))
        {
            message = MessageUtils.message(code, args);
        }
        if (message == null)
        {
            message = defaultMessage;
        }
        return message;
    }

    /**
     * 获取所属模块
     * @return 模块标识
     */
    public String getModule()
    {
        return module;
    }

    /**
     * 获取错误码
     * @return 错误码字符串
     */
    public String getCode()
    {
        return code;
    }

    /**
     * 获取消息模板参数
     * @return 参数数组
     */
    public Object[] getArgs()
    {
        return args;
    }

    /**
     * 获取默认错误消息
     * @return 默认消息
     */
    public String getDefaultMessage()
    {
        return defaultMessage;
    }
}
