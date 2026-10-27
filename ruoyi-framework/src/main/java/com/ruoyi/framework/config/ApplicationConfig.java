package com.ruoyi.framework.config;

import java.util.TimeZone;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 应用启动基础配置 —— AOP 代理暴露 + Mapper 扫描 + Jackson 时区
 *
 * <p>设计意图：作为整个 Spring Boot 应用启动的基础配置类，
 * 提供三个关键配置：</p>
 * <ol>
 *   <li>{@link EnableAspectJAutoProxy}(exposeProxy = true) —— 暴露 AOP 代理对象到 ThreadLocal，
 *       使得在同一个类的内部方法调用时也能通过 {@code AopContext.currentProxy()} 获取代理对象，
 *       解决 Spring AOP 自调用（self-invocation）失效问题</li>
 *   <li>{@link MapperScan} —— 扫描 MyBatis Mapper 接口包路径 {@code com.ruoyi.**.mapper}，
 *       将所有 Mapper 接口注册为 Spring Bean，供 Service 层注入使用</li>
 *   <li>{@link Jackson2ObjectMapperBuilderCustomizer} —— 统一 Jackson 序列化时区为系统默认时区，
 *       避免在不同部署环境下出现日期时间差</li>
 * </ol>
 *
 * @author ruoyi
 * @see EnableAspectJAutoProxy Spring AOP 代理配置注解
 * @see MapperScan MyBatis Mapper 扫描注解
 */
@Configuration
// 表示通过aop框架暴露该代理对象,AopContext能够访问
@EnableAspectJAutoProxy(exposeProxy = true)
// 指定要扫描的Mapper类的包的路径
@MapperScan("com.ruoyi.**.mapper")
public class ApplicationConfig
{
    /**
     * Jackson 序列化时区配置。
     *
     * <p>将所有 JSON 序列化/反序列化的时区设为系统默认时区（JVM {@link TimeZone#getDefault}），
     * 避免跨时区服务器部署时出现时间偏移。</p>
     *
     * @return Jackson Builder 自定义器 Bean
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonObjectMapperCustomization()
    {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder.timeZone(TimeZone.getDefault());
    }
}
