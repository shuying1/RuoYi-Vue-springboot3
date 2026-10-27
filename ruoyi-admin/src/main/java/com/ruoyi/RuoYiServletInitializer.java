package com.ruoyi;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * WAR 包部署入口类
 * 
 * <p>当项目以 WAR 形式部署到外部 Servlet 容器（Tomcat、Jetty 等）时，
 * 由容器通过 SPI 机制发现本类并调用 {@link #configure}，将 {@link RuoYiApplication}
 * 注册为嵌入式 Web 应用的配置源，从而在传统 WAR 部署模式下依然保持 Spring Boot
 * 的自动装配与外部化配置能力。</p>
 * 
 * <p>如果使用内嵌容器（JAR 启动），此类不会被加载，由 {@code main} 方法直接启动。</p>
 * 
 * @author ruoyi
 * @see RuoYiApplication
 */
public class RuoYiServletInitializer extends SpringBootServletInitializer {
    
    /**
     * 指定 Spring Boot 主配置源
     * 
     * @param application Spring Boot 的应用构建器，由外部 Servlet 容器创建
     * @return 注册了 {@link RuoYiApplication} 配置类的构建器实例
     */
    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(RuoYiApplication.class);
    }
}
