package com.ruoyi.common.exception.file;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * 文件上传异常类
 * 
 * 继承受检异常 Exception，使用自定义的printStackTrace方法来额外输出
 * 嵌套异常的堆栈信息，便于排查文件上传过程中的多层调用链问题。
 *
 * 与 {@link FileException}（运行时异常）不同，此类为受检异常，调用方需要显式处理。
 * 
 * @author ruoyi
 */
public class FileUploadException extends Exception
{

    private static final long serialVersionUID = 1L;

    /**
     * 嵌套的原始异常对象
     */
    private final Throwable cause;

    /**
     * 空构造函数
     */
    public FileUploadException()
    {
        this(null, null);
    }

    /**
     * 构造函数，仅设置错误消息
     * 
     * @param msg 错误提示消息
     */
    public FileUploadException(final String msg)
    {
        this(msg, null);
    }

    /**
     * 全参数构造函数
     * 
     * @param msg   错误消息
     * @param cause 原始异常对象
     */
    public FileUploadException(String msg, Throwable cause)
    {
        super(msg);
        this.cause = cause;
    }

    /**
     * 将异常堆栈输出到PrintStream，额外输出嵌套异常堆栈
     */
    @Override
    public void printStackTrace(PrintStream stream)
    {
        super.printStackTrace(stream);
        if (cause != null)
        {
            stream.println("Caused by:");
            cause.printStackTrace(stream);
        }
    }

    /**
     * 将异常堆栈输出到PrintWriter，额外输出嵌套异常堆栈
     */
    @Override
    public void printStackTrace(PrintWriter writer)
    {
        super.printStackTrace(writer);
        if (cause != null)
        {
            writer.println("Caused by:");
            cause.printStackTrace(writer);
        }
    }

    /**
     * 获取嵌套的原始异常
     * @return 原始Throwable对象
     */
    @Override
    public Throwable getCause()
    {
        return cause;
    }
}
