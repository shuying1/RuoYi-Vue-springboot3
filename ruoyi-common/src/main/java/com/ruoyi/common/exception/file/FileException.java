package com.ruoyi.common.exception.file;

import com.ruoyi.common.exception.base.BaseException;

/**
 * 文件操作相关异常基类
 * 
 * 继承 {@link BaseException}，固定所属模块为 "file"。
 * 文件上传、下载、校验等场景的子异常类都应继承此类。
 * 
 * @author ruoyi
 */
public class FileException extends BaseException
{
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数，指定错误码和消息参数
     * 自动将模块设为 "file"
     * 
     * @param code 错误码（在资源文件中定义）
     * @param args 消息模板参数
     */
    public FileException(String code, Object[] args)
    {
        super("file", code, args, null);
    }

}
