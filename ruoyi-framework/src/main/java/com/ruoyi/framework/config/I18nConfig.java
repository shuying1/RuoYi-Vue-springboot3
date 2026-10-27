package com.ruoyi.framework.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;
import com.ruoyi.common.constant.Constants;

/**
 * 国际化 i18n 配置 —— LocaleResolver + 语言切换拦截器
 *
 * <p>设计意图：为应用提供国际化（多语言）支持：</p>
 * <ol>
 *   <li>{@link SessionLocaleResolver} —— 基于 HttpSession 的 Locale 存储，
 *       默认语言通过 {@link Constants#DEFAULT_LOCALE}（通常是 zh_CN）指定；
 *       用户切换语言后，Locale 存入 Session 中，后续请求自动使用该语言</li>
 *   <li>{@link LocaleChangeInterceptor} —— 请求参数拦截器，
 *       当 URL 中包含 {@code lang} 参数（如 {@code ?lang=en_US}）时自动切换 Session 中的 Locale</li>
 * </ol>
 *
 * <p>国际化资源文件位于 classpath 的 {@code messages*.properties}（由 Spring Boot 自动加载）。</p>
 *
 * @author ruoyi
 * @see SessionLocaleResolver 基于 Session 的 Locale 存储
 * @see LocaleChangeInterceptor 请求参数触发语言切换
 * @see WebMvcConfigurer#addInterceptors 拦截器注册
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer
{
    /**
     * 基于 HttpSession 的 Locale 解析器。
     *
     * <p>默认语言设为 {@link Constants#DEFAULT_LOCALE}，
     * 用户可以通过 URL 参数 lang=en_US 切换（由 localeChangeInterceptor 处理）。</p>
     *
     * @return SessionLocaleResolver Bean
     */
    @Bean
    public LocaleResolver localeResolver()
    {
        SessionLocaleResolver slr = new SessionLocaleResolver();
        // 默认语言
        slr.setDefaultLocale(Constants.DEFAULT_LOCALE);
        return slr;
    }

    /**
     * 语言切换拦截器。
     *
     * <p>当 URL 中包含 {@code lang} 参数（如 ?lang=en_US）时，
     * 自动将 Session 中的 Locale 设置为对应值。</p>
     *
     * @return LocaleChangeInterceptor Bean
     */
    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor()
    {
        LocaleChangeInterceptor lci = new LocaleChangeInterceptor();
        // 参数名
        lci.setParamName("lang");
        return lci;
    }

    /**
     * 向 Spring MVC 拦截器注册表中添加语言切换拦截器。
     *
     * <p>作用范围：所有 Controller 请求。</p>
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
