package com.ruoyi.common.exception.file;

/**
 * 文件大小超限异常类
 * 
 * 当上传文件超过系统配置的最大大小限制时抛出此异常。
 * 通过资源文件消息模板 "upload.exceed.maxSize" 传入允许的最大大小参数（单位字节）。
 * 
 * @author ruoyi
 */
public class FileSizeLimitExceededException extends FileException
{
    private static final long serialVersionUID = 1L;

    /**
     * 构造函数
     * 
     * @param defaultMaxSize 系统允许的默认文件最大大小（字节）
     */
    public FileSizeLimitExceededException(long defaultMaxSize)
    {
        super("upload.exceed.maxSize", new Object[] { defaultMaxSize });
    }
}
