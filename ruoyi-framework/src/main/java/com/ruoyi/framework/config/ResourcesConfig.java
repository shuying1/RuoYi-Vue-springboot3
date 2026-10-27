package com.ruoyi.framework.config;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 通用 Web MVC 配置 —— 静态资源 + 防重提交拦截器 + CORS 跨域
 *
 * <p>设计意图：作为 Spring MVC 的 {@link WebMvcConfigurer} 实现，
 * 提供三个核心配置：</p>
 * <ol>
 *   <li><b>静态资源映射</b> —— 将上传文件目录（profile）和 Swagger UI 的 WebJars 资源
 *       映射为可通过 HTTP 直接访问的 URL 路径</li>
 *   <li><b>防重复提交拦截器</b> —— 注册 {@link RepeatSubmitInterceptor}，
 *       拦截所有标注 {@link com.ruoyi.common.annotation.RepeatSubmit} 注解的 Controller 方法</li>
 *   <li><b>CORS 跨域配置</b> —— 注册 {@link CorsFilter}，
 *       允许前后端分离架构下跨域请求（开发阶段尤为重要）</li>
 * </ol>
 *
 * @author ruoyi
 * @see RepeatSubmitInterceptor 防重提交拦截器（Spring MVC 层实现）
 * @see CorsFilter 跨域过滤器（Spring Security 过滤器链之前执行）
 * @see RuoYiConfig 系统全局配置（profile 路径等）
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer
{
    /** 防重复提交拦截器 Bean，由 Spring 自动注入 */
    @Autowired
    private RepeatSubmitInterceptor repeatSubmitInterceptor;

    /**
     * 配置静态资源处理器。
     *
     * <p>两个关键映射：</p>
     * <ul>
     *   <li>{@code /profile/**} → {@code file:} + RuoYiConfig.getProfile() —— 本地文件上传目录，
     *       使得通过 HTTP 可以直接访问用户上传的头像、文档等静态资源</li>
     *   <li>{@code /swagger-ui/**} → classpath 中的 springfox-swagger-ui WebJar ——
     *       Swagger API 文档 UI 资源，设置 5 小时 HTTP 缓存</li>
     * </ul>
     *
     * @param registry Spring MVC 资源处理器注册表
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        /** 本地文件上传路径 */
        registry.addResourceHandler(Constants.RESOURCE_PREFIX + "/**")
                .addResourceLocations("file:" + RuoYiConfig.getProfile() + "/");

        /** swagger配置 */
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/springfox-swagger-ui/")
                .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
    }

    /**
     * 注册自定义拦截器。
     *
     * <p>注册 {@link RepeatSubmitInterceptor} 拦截所有 URL（/**），
     * 拦截器内部会检查目标方法是否标注 {@link com.ruoyi.common.annotation.RepeatSubmit} 注解，
     * 未标注的方法直接放行。</p>
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(repeatSubmitInterceptor).addPathPatterns("/**");
    }

    /**
     * 配置 CORS 跨域过滤器 Bean。
     *
     * <p>配置策略：允许所有源（allowedOriginPattern="*"）、所有请求头、所有请求方法，
     * 预检请求（OPTIONS）有效期 1800 秒（浏览器不重复发送预检）。</p>
     *
     * <p>注意：本 {@link CorsFilter} Bean 会被 {@link SecurityConfig} 注入到 Spring Security 过滤器链中，
     * 确保在 JWT 过滤器之前执行，避免跨域预检请求被 JWT 校验拦截。</p>
     *
     * @return 配置好的 CorsFilter 实例
     */
    @Bean
    public CorsFilter corsFilter()
    {
        CorsConfiguration config = new CorsConfiguration();
        // 设置访问源地址
        config.addAllowedOriginPattern("*");
        // 设置访问源请求头
        config.addAllowedHeader("*");
        // 设置访问源请求方法
        config.addAllowedMethod("*");
        // 有效期 1800秒
        config.setMaxAge(1800L);
        // 添加映射路径，拦截一切请求
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        // 返回新的CorsFilter
        return new CorsFilter(source);
    }
}
