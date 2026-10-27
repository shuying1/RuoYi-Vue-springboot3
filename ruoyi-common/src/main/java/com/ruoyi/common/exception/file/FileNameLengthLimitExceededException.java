package com.ruoyi.common.exception.file;

/**
 * 文件名称超长限制异常类
 * 
 * 当上传文件名超过系统配置的最大长度限制时抛出此异常。
 * 通过资源文件消息模板 "upload.filename.exceed.length" 传入允许的最大长度参数。
 * 
 * @author ruoyi
 */
public class FileNameLengthLimitExceededException extends FileException
{
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     * 
     * @param defaultFileNameLength 系统允许的默认文件名最大长度
     */
    public FileNameLengthLimitExceededException(int defaultFileNameLength)
    {
        super("upload.filename.exceed.length", new Object[] { defaultFileNameLength });
    }
}
