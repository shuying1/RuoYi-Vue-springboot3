package com.ruoyi.common.core.redis;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * Spring Redis缓存操作工具类
 * 
 * 封装 Spring Data Redis 的 {@link RedisTemplate} 常用操作，提供对 String、List、Set、Hash 
 * 等 Redis 数据结构的便捷访问方法。支持设置过期时间、批量删除、Hash字段操作等。
 *
 * 该类作为Spring Bean自动注册，通过 @Autowired 注入使用。
 * 统一使用泛型 T 保证类型安全，内部会自动处理类型转换。
 *
 * 典型用法：
 * <pre>{@code
 *   @Autowired
 *   private RedisCache redisCache;
 *   redisCache.setCacheObject("key", value, 30, TimeUnit.MINUTES);
 *   Object cached = redisCache.getCacheObject("key");
 * }</pre>
 *
 * @author ruoyi
 */
@SuppressWarnings(value = { "unchecked", "rawtypes" })
@Component
public class RedisCache
{
    /**
     * Spring Data Redis 模板对象，所有操作的底层实现
     */
    @Autowired
    public RedisTemplate redisTemplate;

    /**
     * 缓存基本对象（Integer、String、实体类等），不设置过期时间
     *
     * @param key   Redis键
     * @param value 缓存的值
     * @param <T>   值类型
     */
    public <T> void setCacheObject(final String key, final T value)
    {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 缓存基本对象并设置过期时间
     *
     * @param key      Redis键
     * @param value    缓存的值
     * @param timeout  过期时间数值
     * @param timeUnit 过期时间单位
     * @param <T>      值类型
     */
    public <T> void setCacheObject(final String key, final T value, final Integer timeout, final TimeUnit timeUnit)
    {
        redisTemplate.opsForValue().set(key, value, timeout, timeUnit);
    }

    /**
     * 设置键的过期时间（单位秒）
     *
     * @param key     Redis键
     * @param timeout 过期时间（秒）
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout)
    {
        return expire(key, timeout, TimeUnit.SECONDS);
    }

    /**
     * 设置键的过期时间（自定义时间单位）
     *
     * @param key    Redis键
     * @param timeout 过期时间数值
     * @param unit   时间单位
     * @return true=设置成功；false=设置失败
     */
    public boolean expire(final String key, final long timeout, final TimeUnit unit)
    {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取键的剩余有效时间（单位秒）
     *
     * @param key Redis键
     * @return 剩余过期秒数；-1表示永久有效；-2表示键不存在
     */
    public long getExpire(final String key)
    {
        return redisTemplate.getExpire(key);
    }

    /**
     * 判断指定键是否存在于Redis中
     *
     * @param key Redis键
     * @return true=存在 false=不存在
     */
    public Boolean hasKey(String key)
    {
        return redisTemplate.hasKey(key);
    }

    /**
     * 获取缓存的基本对象值
     *
     * @param key Redis键
     * @return 缓存值，不存在时返回null
     */
    public <T> T getCacheObject(final String key)
    {
        ValueOperations<String, T> operation = redisTemplate.opsForValue();
        return operation.get(key);
    }

    /**
     * 删除单个缓存键
     *
     * @param key 待删除的Redis键
     * @return true=删除成功
     */
    public boolean deleteObject(final String key)
    {
        return redisTemplate.delete(key);
    }

    /**
     * 批量删除缓存键
     *
     * @param collection 待删除的键集合
     * @return 删除的键数量是否大于0
     */
    public boolean deleteObject(final Collection collection)
    {
        return redisTemplate.delete(collection) > 0;
    }

    /**
     * 缓存List数据，从右侧批量推入
     *
     * @param key      Redis键
     * @param dataList 待缓存的List数据
     * @return 推入的元素数量
     */
    public <T> long setCacheList(final String key, final List<T> dataList)
    {
        Long count = redisTemplate.opsForList().rightPushAll(key, dataList);
        return count == null ? 0 : count;
    }

    /**
     * 获取缓存的List对象（返回全部元素）
     *
     * @param key Redis键
     * @return List中的所有元素；不存在时返回空List
     */
    public <T> List<T> getCacheList(final String key)
    {
        return redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * 缓存Set数据，遍历逐个添加到Redis Set
     *
     * @param key     Redis键
     * @param dataSet 待缓存的Set数据
     * @return 绑定的Set操作对象，可继续操作
     */
    public <T> BoundSetOperations<String, T> setCacheSet(final String key, final Set<T> dataSet)
    {
        BoundSetOperations<String, T> setOperation = redisTemplate.boundSetOps(key);
        Iterator<T> it = dataSet.iterator();
        while (it.hasNext())
        {
            setOperation.add(it.next());
        }
        return setOperation;
    }

    /**
     * 获取缓存的Set集合（所有成员）
     *
     * @param key Redis键
     * @return Set中的所有成员
     */
    public <T> Set<T> getCacheSet(final String key)
    {
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 缓存Map数据，使用Hash结构存储
     * 传入Map为null时不执行操作
     *
     * @param key     Redis键
     * @param dataMap 待缓存的Map数据
     */
    public <T> void setCacheMap(final String key, final Map<String, T> dataMap)
    {
        if (dataMap != null) {
            redisTemplate.opsForHash().putAll(key, dataMap);
        }
    }

    /**
     * 获取缓存的Map数据（返回Hash中所有字段和值）
     *
     * @param key Redis键
     * @return Hash结构的完整Map
     */
    public <T> Map<String, T> getCacheMap(final String key)
    {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * 向Hash结构中存储单个字段值
     *
     * @param key  Redis键（Hash键）
     * @param hKey Hash中的字段名
     * @param value 字段值
     */
    public <T> void setCacheMapValue(final String key, final String hKey, final T value)
    {
        redisTemplate.opsForHash().put(key, hKey, value);
    }

    /**
     * 获取Hash结构中的单个字段值
     *
     * @param key  Redis键
     * @param hKey Hash字段名
     * @return 字段值；不存在时返回null
     */
    public <T> T getCacheMapValue(final String key, final String hKey)
    {
        HashOperations<String, String, T> opsForHash = redisTemplate.opsForHash();
        return opsForHash.get(key, hKey);
    }

    /**
     * 批量获取Hash结构中多个字段的值
     *
     * @param key  Redis键
     * @param hKeys Hash字段名集合
     * @return 与hKeys对应的字段值列表
     */
    public <T> List<T> getMultiCacheMapValue(final String key, final Collection<Object> hKeys)
    {
        return redisTemplate.opsForHash().multiGet(key, hKeys);
    }

    /**
     * 删除Hash结构中的单个字段
     *
     * @param key  Redis键
     * @param hKey 待删除的Hash字段名
     * @return true=删除成功（字段存在）
     */
    public boolean deleteCacheMapValue(final String key, final String hKey)
    {
        return redisTemplate.opsForHash().delete(key, hKey) > 0;
    }

    /**
     * 根据key前缀查找所有匹配的Redis键
     * 
     * @param pattern 键匹配模式，如 "login_tokens:*"
     * @return 所有匹配的键集合
     */
    public Collection<String> keys(final String pattern)
    {
        return redisTemplate.keys(pattern);
    }
}
