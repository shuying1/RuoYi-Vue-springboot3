package com.ruoyi.generator.util;

import java.util.Properties;
import org.apache.velocity.app.Velocity;
import com.ruoyi.common.constant.Constants;

/**
 * Apache Velocity 引擎初始化器
 *
 * <p>在项目中的角色：为整个代码生成器提供 VelocityEngine 单例。
 * 仅暴露一个静态方法 {@link #initVelocity}，被
 * {@link com.ruoyi.generator.service.GenTableServiceImpl} 在每次预览/生成代码前调用。</p>
 *
 * <p>配置要点：</p>
 * <ul>
 *   <li>{@code resource.loader.file.class = ClasspathResourceLoader} ——
 *       从 classpath 加载 vm 模板文件（{@code vm/java/*.vm}、{@code vm/xml/*.vm}、
 *       {@code vm/vue/**/*.vm} 等），不支持外部文件，安全边界清晰；</li>
 *   <li>{@code input.encoding = UTF-8} —— 统一字符集，避免中文模板乱码。</li>
 * </ul>
 *
 * <p>注意：VelocityEngine 是进程级单例，多次调用 init 不会重复创建，
 * 只是确保配置被应用一次。初始化失败时直接 {@code throw new RuntimeException}，
 * 让上层 Controller 捕获后转为友好错误提示。</p>
 *
 * @author ruoyi
 */
public class VelocityInitializer
{
    /**
     * 初始化 VelocityEngine——设置 classpath 加载器和 UTF-8 编码。
     *
     * <p>只需在应用生命周期内调用一次即可。
     * 调用方通常是 {@link com.ruoyi.generator.service.GenTableServiceImpl}
     * 的 previewCode / generatorCode 等方法。</p>
     *
     * @throws RuntimeException VelocityEngine 初始化失败时包装抛出
     */
    public static void initVelocity()
    {
        Properties p = new Properties();
        try
        {
            // 加载classpath目录下的vm文件
            p.setProperty("resource.loader.file.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
            // 定义字符集
            p.setProperty(Velocity.INPUT_ENCODING, Constants.UTF8);
            // 初始化Velocity引擎，指定配置Properties
            Velocity.init(p);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
