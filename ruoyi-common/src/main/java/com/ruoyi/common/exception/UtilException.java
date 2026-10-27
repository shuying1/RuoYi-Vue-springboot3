package com.ruoyi.common.exception;

/**
 * 工具类执行异常
 * 
 * 继承 RuntimeException，用于工具类内部抛出的运行时错误。
 * 支持携带原始异常堆栈信息，便于定位工具类调用链中的问题。
 * 
 * @author ruoyi
 */
public class UtilException extends RuntimeException
{
    private static final long serialVersionUID = 8247610319171014183L;

    /**
     * 构造函数，从Throwable中提取消息和堆栈
     * 
     * @param e 原始异常对象
     */
    public UtilException(Throwable e)
    {
        super(e.getMessage(), e);
    }

    /**
     * 构造函数，仅设置错误消息
     * 
     * @param message 错误提示消息
     */
    public UtilException(String message)
    {
        super(message);
    }

    /**
     * 构造函数，同时设置错误消息和原始异常
     * 
     * @param message   错误提示消息
     * @param throwable 原始异常对象
     */
    public UtilException(String message, Throwable throwable)
    {
        super(message, throwable);
    }
}
