package com.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * RuoYi 主启动类
 * 
 * <p>本类是整个 RuoYi 后端微服务的入口，负责启动 Spring Boot 应用上下文。
 * 框架采用多数据源动态配置方案（见 ruoyi-common 中的 DruidConfig 与动态路由配置），
 * 因此必须在启动时排除 Spring Boot 默认的 {@link DataSourceAutoConfiguration}，
 * 交由自定义的数据源装配逻辑按业务路由（主库/从库）动态注册连接池。</p>
 * 
 * <ul>
 *   <li>{@code exclude = DataSourceAutoConfiguration.class} — 屏蔽默认单数据源自动装配，
 *       避免与自定义的 {@code DynamicDataSource} 冲突。</li>
 *   <li>{@code scanBasePackages} 未显式声明，默认从本类所在包 {@code com.ruoyi} 向下扫描，
 *       可覆盖 admin、generator、framework、common 等子模块的 Bean。</li>
 * </ul>
 * 
 * @author ruoyi
 */
@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class RuoYiApplication
{
    /**
     * Spring Boot 标准启动入口
     * 
     * @param args 命令行传入的启动参数，通常用于覆盖外部化配置（如 --server.port=8080）
     */
    public static void main(String[] args)
    {
        // System.setProperty("spring.devtools.restart.enabled", "false");
        SpringApplication.run(RuoYiApplication.class, args);

    }
}
