package com.ruoyi.framework.config.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.apache.commons.lang3.RegExUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import com.ruoyi.common.annotation.Anonymous;

/**
 * 扫描并注册 @Anonymous 注解 URL —— 自动加入匿名访问白名单
 *
 * <p>设计意图：实现 {@link InitializingBean} + {@link ApplicationContextAware}，
 * 在 Spring Boot 启动完成、所有 Controller 注册后，
 * 自动扫描所有 HandlerMapping 中带 {@link Anonymous} 注解的 Controller 方法和类，
 * 将对应的 URL 模式加入匿名访问白名单，供 {@link com.ruoyi.framework.config.SecurityConfig}
 * 使用（{@code permitAllUrl.getUrls()}）。</p>
 *
 * <p>处理流程：</p>
 * <ol>
 *   <li>{@link #afterPropertiesSet} —— Spring 容器初始化完成后触发</li>
 *   <li>从 ApplicationContext 获取 {@link RequestMappingHandlerMapping} Bean（Spring MVC 默认注册）</li>
 *   <li>遍历所有已注册的 Controller 方法，检查方法和类上是否有 {@link Anonymous} 注解</li>
 *   <li>将 Controller 的路径参数（如 {@code {userId}}）替换为通配符 {@code *}，
 *       以便 Spring Security 的 permitAll URL 能匹配请求</li>
 *   <li>收集到 urls 列表中，后续由 SecurityConfig 注入到 Spring Security 过滤器链</li>
 * </ol>
 *
 * <p>与 {@code SecurityConfig.authorizeHttpRequests} 的关系：
 * 本类提供的 URL 是 Spring Security 过滤器链中匿名访问规则的数据来源之一，
 * 另一个来源是显式配置的 /login、/register、/captchaImage 等。</p>
 *
 * @author ruoyi
 * @see Anonymous 允许匿名访问的注解（标注在 Controller 方法或类上）
 * @see com.ruoyi.framework.config.SecurityConfig#filterChain 消费本类的 urls
 */
@Configuration
public class PermitAllUrlProperties implements InitializingBean, ApplicationContextAware
{
    /** 正则：匹配 Spring MVC 路径参数（如 {userId}、{id}） */
    private static final Pattern PATTERN = Pattern.compile("\\{(.*?)\\}");

    /** Spring 应用上下文，用于获取 RequestMappingHandlerMapping Bean */
    private ApplicationContext applicationContext;

    /** 扫描到的匿名访问 URL 列表（路径参数已替换为 *） */
    private List<String> urls = new ArrayList<>();

    /** 路径参数替换目标值：将 {xxx} 替换为 * 以匹配 Spring Security 的 Ant 风格 URL */
    public String ASTERISK = "*";

    /**
     * Spring 容器初始化完成后执行 —— 扫描所有带 @Anonymous 注解的 URL。
     *
     * <p>扫描逻辑：从 RequestMappingHandlerMapping 获取所有已注册的 Controller 方法，
     * 分别检查方法级和类级 {@link Anonymous} 注解，
     * 然后将路径参数替换为 * 后加入 urls 列表。</p>
     */
    @Override
    public void afterPropertiesSet()
    {
        RequestMappingHandlerMapping mapping = applicationContext.getBean("requestMappingHandlerMapping", RequestMappingHandlerMapping.class);
        Map<RequestMappingInfo, HandlerMethod> map = mapping.getHandlerMethods();

        map.keySet().forEach(info -> {
            HandlerMethod handlerMethod = map.get(info);

            // 获取方法上边的注解 替代path variable 为 *
            Anonymous method = AnnotationUtils.findAnnotation(handlerMethod.getMethod(), Anonymous.class);
            Optional.ofNullable(method).ifPresent(anonymous -> Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues()) //
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));

            // 获取类上边的注解, 替代path variable 为 *
            Anonymous controller = AnnotationUtils.findAnnotation(handlerMethod.getBeanType(), Anonymous.class);
            Optional.ofNullable(controller).ifPresent(anonymous -> Objects.requireNonNull(info.getPathPatternsCondition().getPatternValues())
                    .forEach(url -> urls.add(RegExUtils.replaceAll(url, PATTERN, ASTERISK))));
        });
    }

    /**
     * 设置 ApplicationContext（Spring 容器注入回调）。
     *
     * @param context Spring 应用上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException
    {
        this.applicationContext = context;
    }

    /**
     * 获取扫描到的所有匿名访问 URL。
     *
     * <p>由 {@link com.ruoyi.framework.config.SecurityConfig#filterChain} 在构建过滤器链时调用。</p>
     *
     * @return 匿名访问 URL 列表（路径参数已替换为 *）
     */
    public List<String> getUrls()
    {
        return urls;
    }

    /**
     * 设置匿名访问 URL 列表（主要用于单元测试或手动覆盖）。
     *
     * @param urls 新的 URL 列表
     */
    public void setUrls(List<String> urls)
    {
        this.urls = urls;
    }
}
