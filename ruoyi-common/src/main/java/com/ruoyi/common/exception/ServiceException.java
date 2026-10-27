package com.ruoyi.common.exception;

/**
 * 业务逻辑异常
 * 
 * 继承 RuntimeException，用于业务层抛出可预期的业务错误。
 * 支持携带错误码、错误消息和内部调试明细消息。
 * 被全局异常处理器捕获后转换为友好的API错误响应。
 *
 * 是final类，禁止继承。
 * 
 * @author ruoyi
 */
public final class ServiceException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 自定义错误码，通常对应HTTP状态码或业务错误码
     */
    private Integer code;

    /**
     * 错误提示消息（面向用户的友好描述）
     */
    private String message;

    /**
     * 错误明细，内部调试时使用的详细信息
     * 设计参考 {@code CommonResult#getDetailMessage()}，
     * 便于排查问题但不直接暴露给前端用户
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化时抛出异常
     */
    public ServiceException()
    {
    }

    /**
     * 构造函数，仅设置错误消息
     * 
     * @param message 错误提示消息
     */
    public ServiceException(String message)
    {
        this.message = message;
    }

    /**
     * 构造函数，同时设置错误消息和错误码
     * 
     * @param message 错误提示消息
     * @param code    自定义错误码
     */
    public ServiceException(String message, Integer code)
    {
        this.message = message;
        this.code = code;
    }

    /**
     * 获取详细调试消息
     * @return 内部调试明细
     */
    public String getDetailMessage()
    {
        return detailMessage;
    }

    /**
     * 获取错误消息
     * 
     * @return 面向用户的错误提示，重写父类getMessage()返回this.message
     */
    @Override
    public String getMessage()
    {
        return message;
    }

    /**
     * 获取错误码
     * @return 自定义错误码
     */
    public Integer getCode()
    {
        return code;
    }

    /**
     * 设置错误消息（支持链式调用）
     * 
     * @param message 错误提示消息
     * @return 当前异常对象
     */
    public ServiceException setMessage(String message)
    {
        this.message = message;
        return this;
    }

    /**
     * 设置错误明细（支持链式调用）
     * 
     * @param detailMessage 详细调试信息
     * @return 当前异常对象
     */
    public ServiceException setDetailMessage(String detailMessage)
    {
        this.detailMessage = detailMessage;
        return this;
    }
}
