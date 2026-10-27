package com.ruoyi.generator.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

/**
 * 代码生成器全局配置
 *
 * <p>在项目中的角色：加载代码生成器的全局参数——作者、包路径、表前缀、
 * 是否覆盖本地文件等。配置来源优先级：
 * {@code application.yml → generator.yml}（后者通过 {@code @PropertySource} 覆盖）。</p>
 *
 * <p>特殊设计：本类字段全是 {@code static}，setter 用 {@code @Value} 注入后
 * 再赋值给静态字段。这样 {@link GenUtils}、{@link GenTableServiceImpl} 等工具类
 * 可以直接通过静态方法 {@code GenConfig.getAuthor()} 读取配置，
 * 无需走 Spring 容器查找。</p>
 *
 * @author ruoyi
 */
@Component
@ConfigurationProperties(prefix = "gen")
@PropertySource(value = { "classpath:generator.yml" })
public class GenConfig
{
    /** 作者——写入生成代码的 @author 注释 */
    public static String author;

    /** 生成包路径——如 com.ruoyi.system，作为默认 packageName */
    public static String packageName;

    /** 是否自动去除表前缀——导入表结构时是否剥离 sys_ / biz_ 等前缀 */
    public static boolean autoRemovePre;

    /** 表前缀——逗号分隔多个，如 "sys_,biz_,gen_" */
    public static String tablePrefix;

    /** 是否允许生成的文件直接覆盖本地项目路径——由管理员手动控制，防止误覆盖 */
    public static boolean allowOverwrite;

    /** @return 生成代码的作者名称 */
    public static String getAuthor()
    {
        return author;
    }

    /** @param author 作者名（由 @Value 注入） */
    @Value("${author}")
    public void setAuthor(String author)
    {
        GenConfig.author = author;
    }

    /** @return 默认生成包路径 */
    public static String getPackageName()
    {
        return packageName;
    }

    /** @param packageName 包路径（由 @Value 注入） */
    @Value("${packageName}")
    public void setPackageName(String packageName)
    {
        GenConfig.packageName = packageName;
    }

    /** @return 是否自动去除表前缀 */
    public static boolean getAutoRemovePre()
    {
        return autoRemovePre;
    }

    /** @param autoRemovePre 是否去除前缀（由 @Value 注入） */
    @Value("${autoRemovePre}")
    public void setAutoRemovePre(boolean autoRemovePre)
    {
        GenConfig.autoRemovePre = autoRemovePre;
    }

    /** @return 表前缀字符串 */
    public static String getTablePrefix()
    {
        return tablePrefix;
    }

    /** @param tablePrefix 表前缀（由 @Value 注入） */
    @Value("${tablePrefix}")
    public void setTablePrefix(String tablePrefix)
    {
        GenConfig.tablePrefix = tablePrefix;
    }

    /** @return 是否允许覆盖本地文件 */
    public static boolean isAllowOverwrite()
    {
        return allowOverwrite;
    }

    /** @param allowOverwrite 是否允许覆盖（由 @Value 注入） */
    @Value("${allowOverwrite}")
    public void setAllowOverwrite(boolean allowOverwrite)
    {
        GenConfig.allowOverwrite = allowOverwrite;
    }
}
