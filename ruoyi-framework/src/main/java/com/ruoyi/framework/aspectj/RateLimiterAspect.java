package com.ruoyi.framework.aspectj;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;

/**
 * 接口限流切面
 *
 * <p>设计意图：通过 AOP 拦截所有标注了 {@link RateLimiter} 注解的 Controller 方法，
 * 使用 Redis + Lua 脚本实现原子性的分布式限流（令牌桶 / 固定窗口算法）。
 * 避免单机内存限流在集群部署下不准确的问题。</p>
 *
 * <p>限流流程：</p>
 * <ol>
 *   <li>{@link #doBefore} 在方法执行前触发，读取注解的 {@code time}（时间窗口，秒）和 {@code count}（允许请求数）</li>
 *   <li>通过 {@link #getCombineKey} 构造 Redis Key：{prefix + "-" + (可选 IP) + 类名 + "-" + 方法名}</li>
 *   <li>执行 {@link RedisScript} Lua 脚本（原子操作）：
 *       <ul>
 *         <li>若 Key 不存在 → 设置 Key 和过期时间，计数置 1，返回 1</li>
 *         <li>若 Key 存在且计数 < count → 计数 +1，返回当前计数</li>
 *         <li>若 Key 存在且计数 ≥ count → 返回当前计数（≥ count，触发限流）</li>
 *       </ul>
 *   </li>
 *   <li>若返回值为 null 或 > count，抛出 {@link ServiceException} 拒绝请求</li>
 * </ol>
 *
 * <p>支持的限流粒度：</p>
 * <ul>
 *   <li>{@link LimitType#IP} —— 按客户端 IP 地址限流，相同 IP 共享计数</li>
 *   <li>{@link LimitType#DEFAULT} —— 按接口全局限流（所有请求共享同一个计数）</li>
 * </ul>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link RedisTemplate} —— 执行 Lua 脚本</li>
 *   <li>{@link RedisScript} —— 限流 Lua 脚本（通常在 RedisConfig 中声明为 Spring Bean）</li>
 * </ul>
 *
 * @author ruoyi
 * @see RateLimiter 限流注解
 * @see LimitType 限流粒度枚举
 */
@Aspect
@Component
public class RateLimiterAspect
{
    /** 限流切面日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(RateLimiterAspect.class);

    /** Spring Data Redis 模板，用于执行限流 Lua 脚本 */
    private RedisTemplate<Object, Object> redisTemplate;

    /**
     * 限流 Lua 脚本实例（在 RedisConfig 中定义为 Bean），
     * 保证原子性（判断 + 自增 + 过期时间）。
     */
    private RedisScript<Long> limitScript;

    /**
     * 通过 setter 注入 RedisTemplate（使用非 final + setter 形式，便于单元测试 Mock）。
     *
     * @param redisTemplate Spring Data Redis 模板 Bean
     */
    @Autowired
    public void setRedisTemplate1(RedisTemplate<Object, Object> redisTemplate)
    {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 通过 setter 注入限流 Lua 脚本实例。
     *
     * @param limitScript 在 Spring 容器中定义的 {@link RedisScript} Bean，
     *                    脚本内容需接收 KEYS[1] = 限流 Key，ARGV[1] = count（上限），ARGV[2] = time（窗口秒数）
     */
    @Autowired
    public void setLimitScript(RedisScript<Long> limitScript)
    {
        this.limitScript = limitScript;
    }

    /**
     * 前置通知：在标注了 {@link RateLimiter} 的方法执行前进行限流判断。
     *
     * <p>执行逻辑：</p>
     * <ol>
     *   <li>读取注解的时间窗口 {@code time} 和允许请求数 {@code count}</li>
     *   <li>调用 {@link #getCombineKey} 构造 Redis Key</li>
     *   <li>执行 Lua 脚本，获取当前窗口内已请求次数</li>
     *   <li>若脚本返回 null 或 count 被超限，抛出业务异常 "访问过于频繁，请稍候再试"</li>
     * </ol>
     *
     * @param point AOP 连接点
     * @param rateLimiter 目标方法上的 {@link RateLimiter} 注解
     * @throws ServiceException 超过限流阈值时抛出
     * @throws RuntimeException Redis 执行异常时包装抛出
     */
    @Before("@annotation(rateLimiter)")
    public void doBefore(JoinPoint point, RateLimiter rateLimiter) throws Throwable
    {
        int time = rateLimiter.time();
        int count = rateLimiter.count();

        String combineKey = getCombineKey(rateLimiter, point);
        List<Object> keys = Collections.singletonList(combineKey);
        try
        {
            // 执行限流 Lua 脚本：KEYS[1]=限流Key, ARGV[1]=count(上限), ARGV[2]=time(窗口秒数)
            Long number = redisTemplate.execute(limitScript, keys, count, time);
            if (StringUtils.isNull(number) || number.intValue() > count)
            {
                // 当前请求数超过注解允许的 count，拒绝请求
                throw new ServiceException("访问过于频繁，请稍候再试");
            }
            log.info("限制请求'{}',当前请求'{}',缓存key'{}'", count, number.intValue(), combineKey);
        }
        catch (ServiceException e)
        {
            // 业务限流异常直接向上抛出，不做额外包装
            throw e;
        }
        catch (Exception e)
        {
            // Redis 或脚本执行异常，包装为 RuntimeException 向上抛出
            throw new RuntimeException("服务器限流异常，请稍候再试");
        }
    }

    /**
     * 构造限流 Redis Key。
     *
     * <p>Key 组成规则：</p>
     * <pre>{@code
     * rateLimiter.key() + [IP前缀-] + 类名 + "-" + 方法名
     * }</pre>
     * 例：rate_limit:192.168.1.100-com.ruoyi.web.controller.system.SysUserController-listUser
     *
     * @param rateLimiter RateLimiter 注解实例
     * @param point AOP 连接点，用于获取目标类和方法
     * @return 最终的 Redis Key 字符串
     */
    public String getCombineKey(RateLimiter rateLimiter, JoinPoint point)
    {
        StringBuffer stringBuffer = new StringBuffer(rateLimiter.key());
        // IP 限流模式下，Key 中拼接客户端 IP，实现不同 IP 独立计数
        if (rateLimiter.limitType() == LimitType.IP)
        {
            stringBuffer.append(IpUtils.getIpAddr()).append("-");
        }
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Class<?> targetClass = method.getDeclaringClass();
        stringBuffer.append(targetClass.getName()).append("-").append(method.getName());
        return stringBuffer.toString();
    }
}
