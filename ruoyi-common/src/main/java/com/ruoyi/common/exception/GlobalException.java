package com.ruoyi.common.exception;

/**
 * 全局系统异常
 * 
 * 继承 RuntimeException，用于表示系统级的意外错误（非业务逻辑错误）。
 * 功能与 {@link ServiceException} 类似，但语义上用于区分业务异常和系统异常。
 * 通常由全局异常处理器 {@code GlobalExceptionHandler} 捕获处理。
 * 
 * @author ruoyi
 */
public class GlobalException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 错误提示消息（面向用户的友好描述）
     */
    private String message;

    /**
     * 错误明细，内部调试时使用
     * 设计参考 {@code CommonResult#getDetailMessage()}
     */
    private String detailMessage;

    /**
     * 空构造方法，避免反序列化问题
     */
    public GlobalException()
    {
    }

    /**
     * 构造函数
     * 
     * @param message 错误提示消息
     */
    public GlobalException(String message)
    {
        this.message = message;
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
     * 设置错误明细（支持链式调用）
     * 
     * @param detailMessage 详细调试信息
     * @return 当前异常对象
     */
    public GlobalException setDetailMessage(String detailMessage)
    {
        this.detailMessage = detailMessage;
        return this;
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
     * 设置错误消息（支持链式调用）
     * 
     * @param message 错误提示消息
     * @return 当前异常对象
     */
    public GlobalException setMessage(String message)
    {
        this.message = message;
        return this;
    }
}
