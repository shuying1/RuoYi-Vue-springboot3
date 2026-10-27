package com.ruoyi.framework.config;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.filter.RefererFilter;
import com.ruoyi.common.filter.RepeatableFilter;
import com.ruoyi.common.filter.XssFilter;
import com.ruoyi.common.utils.StringUtils;

/**
 * Servlet Filter 注册配置 —— XSS 防护 + 防盗链 + Body 可重复读
 *
 * <p>设计意图：向 Spring Boot Servlet 容器注册三个 Filter：</p>
 * <ol>
 *   <li><b>XssFilter</b> —— 跨站脚本攻击防护，对请求参数中的特殊字符进行转义 / 替换，
 *       作用于所有 URL（可通过 {@code xss.excludes} 配置排除路径）</li>
 *   <li><b>RefererFilter</b> —— 防盗链，校验请求的 Referer 头是否在允许域名白名单内，
 *       主要保护上传的静态资源（profile 目录）</li>
 *   <li><b>RepeatableFilter</b> —— 包装 HttpServletRequest，使 POST 请求的 Body 可被多次读取，
 *       解决 {@link com.ruoyi.framework.interceptor.impl.SameUrlDataInterceptor} 需要读取 Body 做防重提交校验
 *       但 Body 只能读一次的问题。此 Filter 为系统核心，无条件生效</li>
 * </ol>
 *
 * <p>Filter 执行顺序（Order）：</p>
 * <ul>
 *   <li>XssFilter / RefererFilter：HIGHEST_PRECEDENCE（最先执行）</li>
 *   <li>RepeatableFilter：LOWEST_PRECEDENCE（最后执行）</li>
 * </ul>
 *
 * <p>关键配置：</p>
 * <ul>
 *   <li>{@code xss.enabled} —— 是否开启 XSS 防护（默认开启）</li>
 *   <li>{@code xss.excludes} —— XSS 防护排除的路径（逗号分隔）</li>
 *   <li>{@code xss.urlPatterns} —— XSS 防护作用的 URL Pattern</li>
 *   <li>{@code referer.enabled} —— 是否开启防盗链</li>
 *   <li>{@code referer.allowed-domains} —— 允许的 Referer 域名白名单</li>
 * </ul>
 *
 * @author ruoyi
 * @see XssFilter XSS 防护过滤器
 * @see RefererFilter 防盗链过滤器
 * @see RepeatableFilter Body 可重复读过滤器（防重提交的前置条件）
 */
@Configuration
public class FilterConfig
{
    /** XSS 防护排除的路径（如上传接口、验证码等），从配置 {@code xss.excludes} 读取 */
    @Value("${xss.excludes}")
    private String excludes;

    /** XSS 防护作用的 URL Pattern（如 "/*"、"/api/**"），从配置 {@code xss.urlPatterns} 读取 */
    @Value("${xss.urlPatterns}")
    private String urlPatterns;

    /** 防盗链允许的域名白名单（逗号分隔），从配置 {@code referer.allowed-domains} 读取 */
    @Value("${referer.allowed-domains}")
    private String allowedDomains;

    /**
     * 注册 XSS 防护 Filter —— 对请求参数中的 XSS 攻击字符串进行过滤。
     *
     * <p>仅当配置 {@code xss.enabled=true} 时注册。Filter Order 设为 HIGHEST_PRECEDENCE，
     * 确保在所有业务逻辑（包括 CSRF、Security）之前执行。</p>
     *
     * @return FilterRegistrationBean，Spring Boot 自动注册为 Servlet Filter
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(value = "xss.enabled", havingValue = "true")
    public FilterRegistrationBean xssFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new XssFilter());
        registration.addUrlPatterns(StringUtils.split(urlPatterns, ","));
        registration.setName("xssFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("excludes", excludes);
        registration.setInitParameters(initParameters);
        return registration;
    }

    /**
     * 注册 Referer 防盗链 Filter —— 保护上传的静态资源不被外部站点直接链接。
     *
     * <p>仅当配置 {@code referer.enabled=true} 时注册，
     * 作用范围为 {@link Constants#RESOURCE_PREFIX} + "/*"（通常是 /profile/*）。</p>
     *
     * @return FilterRegistrationBean
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(value = "referer.enabled", havingValue = "true")
    public FilterRegistrationBean refererFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        registration.setFilter(new RefererFilter());
        registration.addUrlPatterns(Constants.RESOURCE_PREFIX + "/*");
        registration.setName("refererFilter");
        registration.setOrder(FilterRegistrationBean.HIGHEST_PRECEDENCE);
        Map<String, String> initParameters = new HashMap<String, String>();
        initParameters.put("allowedDomains", allowedDomains);
        registration.setInitParameters(initParameters);
        return registration;
    }

    /**
     * 注册 RepeatableFilter —— 使 POST 请求 Body 可被多次读取。
     *
     * <p>核心 Filter，无条件注册。作用范围为 "/*"（全站），
     * Order 为 LOWEST_PRECEDENCE（最后执行）。</p>
     *
     * <p>重要性：
     * {@link com.ruoyi.framework.interceptor.impl.SameUrlDataInterceptor} 在 preHandle 阶段
     * 需要读取请求 Body 来判断是否重复提交，但 Servlet 规范中 Body 流只能读一次，
     * 必须由本 Filter 提前包装成 {@link com.ruoyi.common.filter.RepeatedlyRequestWrapper}。</p>
     *
     * @return FilterRegistrationBean
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    public FilterRegistrationBean someFilterRegistration()
    {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new RepeatableFilter());
        registration.addUrlPatterns("/*");
        registration.setName("repeatableFilter");
        registration.setOrder(FilterRegistrationBean.LOWEST_PRECEDENCE);
        return registration;
    }

}
