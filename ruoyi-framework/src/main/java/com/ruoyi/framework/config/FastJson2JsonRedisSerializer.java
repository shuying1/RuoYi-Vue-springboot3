package com.ruoyi.framework.config;

import java.nio.charset.Charset;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.filter.Filter;
import com.ruoyi.common.constant.Constants;

/**
 * 基于 FastJson2 的 Redis 序列化器
 *
 * <p>设计意图：替代 Spring Data Redis 默认的 JDK SerializationRedisSerializer，
 * 将 Redis 中的 Value 以 JSON 格式（UTF-8）存储，
 * 解决 JDK 序列化产生不可读二进制前缀、以及不同 JVM 版本兼容性问题。</p>
 *
 * <p>核心特性：</p>
 * <ul>
 *   <li>序列化时启用 {@link JSONWriter.Feature#WriteClassName}，输出 JSON 中带 {@code @type} 字段标记原始类型，
 *       反序列化时可自动恢复为目标类型</li>
 *   <li>反序列化时使用 {@link JSONReader.autoTypeFilter} 白名单过滤，
 *       防止反序列化安全漏洞（autoType 风险），白名单在 {@link Constants#JSON_WHITELIST_STR} 中配置</li>
 *   <li>统一使用 UTF-8 编码，兼容 Redis 的 String 存储</li>
 * </ul>
 *
 * @param <T> 被序列化/反序列化的对象类型
 * @author ruoyi
 * @see RedisConfig RedisTemplate 中的 Value Serializer 使用本类
 */
public class FastJson2JsonRedisSerializer<T> implements RedisSerializer<T>
{
    /** 默认字符集 —— UTF-8，保证与 Redis 存储兼容 */
    public static final Charset DEFAULT_CHARSET = Charset.forName("UTF-8");

    /**
     * fastjson2 autoType 白名单过滤器，仅允许 JSON 中 @type 指定的类型在白名单列表内才能反序列化，
     * 防止恶意构造的 JSON 触发远程代码执行漏洞。
     * 白名单内容由 {@link Constants#JSON_WHITELIST_STR} 提供（通常包含 com.ruoyi. 包下的类）。
     */
    static final Filter AUTO_TYPE_FILTER = JSONReader.autoTypeFilter(Constants.JSON_WHITELIST_STR);

    /** 目标反序列化类型，由 RedisConfig 构造时传入（通常为 Object.class） */
    private Class<T> clazz;

    /**
     * 构造 FastJson2JsonRedisSerializer。
     *
     * @param clazz 反序列化目标类型，通常传 Object.class 以兼容多种类型对象
     */
    public FastJson2JsonRedisSerializer(Class<T> clazz)
    {
        super();
        this.clazz = clazz;
    }

    /**
     * 将对象序列化为 JSON 字节数组。
     *
     * <p>启用 {@link JSONWriter.Feature#WriteClassName} 输出 @type 字段，
     * 保证反序列化时能恢复原始类型。</p>
     *
     * @param t 待序列化的对象
     * @return JSON 字节数组；若 t 为 null 则返回空数组
     */
    @Override
    public byte[] serialize(T t) throws SerializationException
    {
        if (t == null)
        {
            return new byte[0];
        }
        return JSON.toJSONString(t, JSONWriter.Feature.WriteClassName).getBytes(DEFAULT_CHARSET);
    }

    /**
     * 将 JSON 字节数组反序列化为目标对象。
     *
     * <p>使用 {@link #AUTO_TYPE_FILTER} 白名单过滤 autoType，
     * 防止反序列化安全漏洞。</p>
     *
     * @param bytes Redis 中读取的 JSON 字节数组
     * @return 反序列化后的对象；若 bytes 为 null 或空则返回 null
     */
    @Override
    public T deserialize(byte[] bytes) throws SerializationException
    {
        if (bytes == null || bytes.length <= 0)
        {
            return null;
        }
        String str = new String(bytes, DEFAULT_CHARSET);

        return JSON.parseObject(str, clazz, AUTO_TYPE_FILTER);
    }
}
