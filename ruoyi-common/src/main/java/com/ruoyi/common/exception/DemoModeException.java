package com.ruoyi.common.exception;

/**
 * 演示模式异常
 * 
 * 在系统演示模式下，当用户尝试执行写操作（修改、删除等）时抛出此异常，
 * 阻止演示环境数据被修改。被全局异常处理器捕获后返回友好的提示信息。
 * 
 * @author ruoyi
 */
public class DemoModeException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * 默认构造函数，异常消息由全局异常处理器填充
     */
    public DemoModeException()
    {
    }
}
