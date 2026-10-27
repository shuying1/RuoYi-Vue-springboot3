package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RuoYi 全局配置属性类
 *
 * <p>通过 @ConfigurationProperties(prefix = "ruoyi") 绑定 application.yml 中 ruoyi.* 配置项，
 * 在 Spring 容器启动时自动注入。以静态字段形式暴露项目名称、版本号、版权年份、
 * 文件上传根路径、IP 地址解析开关、验证码类型等全局配置，供任何位置直接引用。
 *
 * <p>提供 getImportPath()、getAvatarPath()、getDownloadPath()、getUploadPath() 四个方法
 * 基于 profile 根路径拼接出各子目录的完整路径，便于文件上传/下载模块统一处理。
 *
 * <p>技术栈：Spring Boot @ConfigurationProperties
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "ruoyi")
public class RuoYiConfig
{
    /** 项目显示名称，用于页脚、登录页等品牌展示 */
    private String name;

    /** 项目版本号 */
    private String version;

    /** 版权年份字符串 */
    private String copyrightYear;

    /** 本地文件存储根目录绝对路径，作为头像、上传文件、导入导出的父目录 */
    private static String profile;

    /** 是否开启根据IP解析归属地功能，true时登录日志会记录IP归属地 */
    private static boolean addressEnabled;

    /** 验证码生成类型，如 math（算术）、char（字符） */
    private static String captchaType;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getCopyrightYear()
    {
        return copyrightYear;
    }

    public void setCopyrightYear(String copyrightYear)
    {
        this.copyrightYear = copyrightYear;
    }

    public static String getProfile()
    {
        return profile;
    }

    public void setProfile(String profile)
    {
        RuoYiConfig.profile = profile;
    }

    public static boolean isAddressEnabled()
    {
        return addressEnabled;
    }

    public void setAddressEnabled(boolean addressEnabled)
    {
        RuoYiConfig.addressEnabled = addressEnabled;
    }

    public static String getCaptchaType() {
        return captchaType;
    }

    public void setCaptchaType(String captchaType) {
        RuoYiConfig.captchaType = captchaType;
    }

    /**
     * 获取导入上传路径
     */
    public static String getImportPath()
    {
        return getProfile() + "/import";
    }

    /**
     * 获取头像上传路径
     */
    public static String getAvatarPath()
    {
        return getProfile() + "/avatar";
    }

    /**
     * 获取下载路径
     */
    public static String getDownloadPath()
    {
        return getProfile() + "/download/";
    }

    /**
     * 获取上传路径
     */
    public static String getUploadPath()
    {
        return getProfile() + "/upload";
    }
}
