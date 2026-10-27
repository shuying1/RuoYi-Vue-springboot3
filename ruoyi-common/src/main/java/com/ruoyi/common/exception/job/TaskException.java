package com.ruoyi.common.exception.job;

/**
 * 定时任务调度异常
 * 
 * 继承 Exception，标记定时任务调度过程中的各类错误。
 * 提供 {@link Code} 枚举区分不同错误场景：
 * TASK_EXISTS（任务已存在）、NO_TASK_EXISTS（任务不存在）、
 * TASK_ALREADY_STARTED（任务已在运行）、UNKNOWN（未知错误）、
 * CONFIG_ERROR（配置错误）、TASK_NODE_NOT_AVAILABLE（调度节点不可用）。
 * 
 * @author ruoyi
 */
public class TaskException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * 错误类型枚举
     */
    private Code code;

    /**
     * 构造函数
     * 
     * @param msg  错误消息
     * @param code 错误类型编码
     */
    public TaskException(String msg, Code code)
    {
        this(msg, code, null);
    }

    /**
     * 全参数构造函数
     * 
     * @param msg        错误消息
     * @param code       错误类型编码
     * @param nestedEx   嵌套的原始异常
     */
    public TaskException(String msg, Code code, Exception nestedEx)
    {
        super(msg, nestedEx);
        this.code = code;
    }

    /**
     * 获取错误类型编码
     * @return Code枚举值
     */
    public Code getCode()
    {
        return code;
    }

    /**
     * 定时任务错误类型枚举
     */
    public enum Code
    {
        /** 任务已存在 */
        TASK_EXISTS,
        /** 任务不存在 */
        NO_TASK_EXISTS,
        /** 任务已在运行 */
        TASK_ALREADY_STARTED,
        /** 未知错误 */
        UNKNOWN,
        /** 任务配置错误 */
        CONFIG_ERROR,
        /** 调度节点不可用 */
        TASK_NODE_NOT_AVAILABLE
    }
}
