package com.ruoyi.common.exception.file;

import java.util.Arrays;

/**
 * 文件扩展名无效异常类
 * 
 * 当上传文件的扩展名不在系统允许列表中时抛出此异常。
 * 提供四个子类用于区分不同文件类型场景（图片、Flash、媒体、视频），
 * 便于上层处理器根据具体类型给出更精确的提示。
 * 
 * @author ruoyi
 */
public class InvalidExtensionException extends FileUploadException
{
    private static final long serialVersionUID = 1L;

    /**
     * 系统允许的有效扩展名列表
     */
    private String[] allowedExtension;

    /**
     * 用户上传的无效扩展名
     */
    private String extension;

    /**
     * 无效的文件名
     */
    private String filename;

    /**
     * 构造函数
     * 
     * @param allowedExtension 系统允许的有效扩展名数组
     * @param extension        用户上传的扩展名
     * @param filename         文件名
     */
    public InvalidExtensionException(String[] allowedExtension, String extension, String filename)
    {
        super("文件[" + filename + "]后缀[" + extension + "]不正确，请上传" + Arrays.toString(allowedExtension) + "格式");
        this.allowedExtension = allowedExtension;
        this.extension = extension;
        this.filename = filename;
    }

    /**
     * 获取允许的扩展名数组
     * @return 允许列表
     */
    public String[] getAllowedExtension()
    {
        return allowedExtension;
    }

    /**
     * 获取无效扩展名
     * @return 用户上传的扩展名
     */
    public String getExtension()
    {
        return extension;
    }

    /**
     * 获取文件名
     * @return 无效的文件名
     */
    public String getFilename()
    {
        return filename;
    }

    /**
     * 图片扩展名无效异常
     */
    public static class InvalidImageExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidImageExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }

    /**
     * Flash文件扩展名无效异常
     */
    public static class InvalidFlashExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidFlashExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }

    /**
     * 媒体文件扩展名无效异常
     */
    public static class InvalidMediaExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidMediaExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }

    /**
     * 视频文件扩展名无效异常
     */
    public static class InvalidVideoExtensionException extends InvalidExtensionException
    {
        private static final long serialVersionUID = 1L;

        public InvalidVideoExtensionException(String[] allowedExtension, String extension, String filename)
        {
            super(allowedExtension, extension, filename);
        }
    }
}
