package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.enums.LimitType;

/**
 * 接口限流注解
 * 
 * 标记在Controller方法上，使用Redis实现接口访问频率限制。
 * 可配置限流时间窗口、最大访问次数和限流类型（全局限流或基于IP限流）。
 * 超过阈值时抛出 ServiceException 阻止请求。
 * 
 * @author ruoyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter
{
    /**
     * 限流Redis键前缀，默认 {@link CacheConstants#RATE_LIMIT_KEY}
     */
    public String key() default CacheConstants.RATE_LIMIT_KEY;

    /**
     * 限流时间窗口，单位秒，默认60秒
     */
    public int time() default 60;

    /**
     * 在限流时间窗口内允许的最大请求次数，默认100次
     */
    public int count() default 100;

    /**
     * 限流类型：DEFAULT-全局限流，IP-基于客户端IP限流
     */
    public LimitType limitType() default LimitType.DEFAULT;
}
