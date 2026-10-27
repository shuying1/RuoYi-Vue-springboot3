package com.ruoyi.framework.config;

import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置 —— 序列化 + 限流脚本
 *
 * <p>设计意图：配置 Spring Boot 与 Redis 交互的核心 Bean：
 * <ul>
 *   <li>{@link RedisTemplate} —— 使用 {@link FastJson2JsonRedisSerializer} 替代 JDK 原生序列化，
 *       Key 使用 {@link StringRedisSerializer}（可读性好、节省内存），Value 使用 fastjson2 JSON 序列化</li>
 *   <li>{@link DefaultRedisScript} —— 限流 Lua 脚本，供 {@link RateLimiterAspect} 原子性执行分布式限流</li>
 * </ul></p>
 *
 * <p>开启 Spring Cache：继承 {@link CachingConfigurerSupport} + {@link EnableCaching}，
 * 允许 Service 层使用 @Cacheable / @CacheEvict 等注解声明式缓存。</p>
 *
 * @author ruoyi
 * @see RedisTemplate 操作 Redis 的核心模板
 * @see FastJson2JsonRedisSerializer fastjson2 实现的 Redis 序列化器
 * @see RateLimiterAspect 使用 limitScript 实现分布式限流
 */
@SuppressWarnings("deprecation")
@Configuration
@EnableCaching
public class RedisConfig extends CachingConfigurerSupport
{
    /**
     * 配置 RedisTemplate Bean —— 自定义 Key/Value 序列化方式。
     *
     * <p>序列化策略：</p>
     * <ul>
     *   <li><b>Key（含 HashKey）</b>：{@link StringRedisSerializer} —— 将 Key 以 UTF-8 编码存储，
     *       避免 JDK 序列化产生的不可读二进制前缀（如 \xAC\xED）</li>
     *   <li><b>Value（含 HashValue）</b>：{@link FastJson2JsonRedisSerializer} —— 将对象序列化为 JSON，
     *       并带上 @type 自动类型，方便反序列化时恢复原始类型</li>
     * </ul>
     *
     * @param connectionFactory Spring Boot 自动配置的 RedisConnectionFactory
     * @return 定制化后的 RedisTemplate Bean
     */
    @Bean
    @SuppressWarnings(value = { "unchecked", "rawtypes" })
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory connectionFactory)
    {
        RedisTemplate<Object, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        FastJson2JsonRedisSerializer serializer = new FastJson2JsonRedisSerializer(Object.class);

        // 使用StringRedisSerializer来序列化和反序列化redis的key值
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);

        // Hash的key也采用StringRedisSerializer的序列化方式
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 分布式限流 Lua 脚本 Bean —— 供 {@link RateLimiterAspect} 执行。
     *
     * <p>脚本逻辑（固定窗口算法）：</p>
     * <ol>
     *   <li>读取 KEYS[1]（限流 Key）的当前计数</li>
     *   <li>若计数存在且 > ARGV[1]（上限 count） → 直接返回当前计数（已超限）</li>
     *   <li>否则计数 INCR，若 INCR 后为 1（首次设置），则 EXPIRE 设为 ARGV[2]（时间窗口 time 秒）</li>
     *   <li>返回当前计数值</li>
     * </ol>
     *
     * <p>原子性：整个脚本在 Redis 中作为单个命令执行，不会出现并发竞争（判断 + 自增 + 过期）。</p>
     *
     * @return DefaultRedisScript Bean，返回值类型为 Long（当前请求计数）
     */
    @Bean
    public DefaultRedisScript<Long> limitScript()
    {
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(limitScriptText());
        redisScript.setResultType(Long.class);
        return redisScript;
    }

    /**
     * 限流 Lua 脚本文本。
     *
     * <p>参数约定：</p>
     * <ul>
     *   <li>KEYS[1] —— 限流 Key（如 rate_limit:IP-类名-方法名）</li>
     *   <li>ARGV[1] —— 允许的最大请求数（count）</li>
     *   <li>ARGV[2] —— 时间窗口（秒，time）</li>
     * </ul>
     *
     * @return Lua 脚本字符串
     */
    private String limitScriptText()
    {
        return "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local time = tonumber(ARGV[2])\n" +
                "local current = redis.call('get', key);\n" +
                "if current and tonumber(current) > count then\n" +
                "    return tonumber(current);\n" +
                "end\n" +
                "current = redis.call('incr', key)\n" +
                "if tonumber(current) == 1 then\n" +
                "    redis.call('expire', key, time)\n" +
                "end\n" +
                "return tonumber(current);";
    }
}
