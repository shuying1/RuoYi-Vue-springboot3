package com.ruoyi.web.controller.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysCache;

/**
 * 缓存监控 Controller
 *
 * <p>业务模块：系统监控 — Redis 缓存管理，路由前缀 {@code /monitor/cache}。
 * 本控制器直接操作 {@link RedisTemplate}，不依赖 Service 层，负责：</p>
 * <ul>
 *   <li>查看 Redis 服务器运行信息（版本、内存、连接数等）与命令执行统计</li>
 *   <li>浏览框架预定义的缓存名称（登录 token、字典、配置、验证码、防重提交、限流、密码错误次数）</li>
 *   <li>按缓存名称查看其下所有 key，以及单个 key 的 value</li>
 *   <li>按名称 / 按 key / 清空全部三种粒度清理缓存</li>
 * </ul>
 *
 * <p>所有接口均要求权限 {@code monitor:cache:list}（清空类接口也使用相同权限）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/cache")
public class CacheController
{
    /** Spring Data Redis 模板，由 framework 模块自动装配 */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /** 框架预注册的缓存名称集合，前端下拉选择器直接使用 */
    private final static List<SysCache> caches = new ArrayList<SysCache>();
    {
        caches.add(new SysCache(CacheConstants.LOGIN_TOKEN_KEY, "用户信息"));
        caches.add(new SysCache(CacheConstants.SYS_CONFIG_KEY, "配置信息"));
        caches.add(new SysCache(CacheConstants.SYS_DICT_KEY, "数据字典"));
        caches.add(new SysCache(CacheConstants.CAPTCHA_CODE_KEY, "验证码"));
        caches.add(new SysCache(CacheConstants.REPEAT_SUBMIT_KEY, "防重提交"));
        caches.add(new SysCache(CacheConstants.RATE_LIMIT_KEY, "限流处理"));
        caches.add(new SysCache(CacheConstants.PWD_ERR_CNT_KEY, "密码错误次数"));
    }

    /**
     * 获取 Redis 服务器运行信息与命令统计
     *
     * <p>url: GET /monitor/cache</p>
     *
     * <p>返回 data 结构：</p>
     * <pre>
     * {
     *   "info":         Properties,        // Redis INFO 命令完整输出
     *   "dbSize":       Long,              // 当前库 key 总数
     *   "commandStats": [ {name, value}, ... ] // TOP 命令调用次数
     * }
     * </pre>
     *
     * @param url 请求路径（GET /monitor/cache）
     * @return AjaxResult 成功时携带 Redis 运行时信息
     * @throws Exception Redis 连接异常时抛出
     */
    @SuppressWarnings("deprecation")
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception
    {
        Properties info = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info());
        Properties commandStats = (Properties) redisTemplate.execute((RedisCallback<Object>) connection -> connection.info("commandstats"));
        Object dbSize = redisTemplate.execute((RedisCallback<Object>) connection -> connection.dbSize());

        Map<String, Object> result = new HashMap<>(3);
        result.put("info", info);
        result.put("dbSize", dbSize);

        List<Map<String, String>> pieList = new ArrayList<>();
        commandStats.stringPropertyNames().forEach(key -> {
            Map<String, String> data = new HashMap<>(2);
            String property = commandStats.getProperty(key);
            data.put("name", StringUtils.removeStart(key, "cmdstat_"));
            data.put("value", StringUtils.substringBetween(property, "calls=", ",usec"));
            pieList.add(data);
        });
        result.put("commandStats", pieList);
        return AjaxResult.success(result);
    }

    /**
     * 获取框架预注册的缓存名称列表
     *
     * <p>url: GET /monitor/cache/getNames</p>
     *
     * @param url 请求路径（GET /monitor/cache/getNames）
     * @return AjaxResult#success(List<SysCache>) 固定 7 个缓存名称条目
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getNames")
    public AjaxResult cache()
    {
        return AjaxResult.success(caches);
    }

    /**
     * 按缓存名称前缀查询所有 key
     *
     * <p>url: GET /monitor/cache/getKeys/{cacheName}</p>
     *
     * <p>底层使用 {@code KEYS cacheName*} 命令，请避免在生产大数据量下频繁调用。</p>
     *
     * @param url 请求路径（GET /monitor/cache/getKeys/{cacheName}）
     * @param cacheName 缓存名称前缀，如 {@code sys_config_key:}
     * @return AjaxResult#success(TreeSet<String>) 去重有序的 key 集合
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getKeys/{cacheName}")
    public AjaxResult getCacheKeys(@PathVariable String cacheName)
    {
        Set<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        return AjaxResult.success(new TreeSet<>(cacheKeys));
    }

    /**
     * 查看指定 key 对应的缓存值
     *
     * <p>url: GET /monitor/cache/getValue/{cacheName}/{cacheKey}</p>
     *
     * @param url 请求路径
     * @param cacheName 缓存名称前缀（仅用于展示，实际查询按 cacheKey）
     * @param cacheKey 完整 Redis key
     * @return AjaxResult#success(SysCache) 包含 cacheName、cacheKey、cacheValue 三字段
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @GetMapping("/getValue/{cacheName}/{cacheKey}")
    public AjaxResult getCacheValue(@PathVariable String cacheName, @PathVariable String cacheKey)
    {
        String cacheValue = redisTemplate.opsForValue().get(cacheKey);
        SysCache sysCache = new SysCache(cacheName, cacheKey, cacheValue);
        return AjaxResult.success(sysCache);
    }

    /**
     * 按缓存名称前缀批量删除
     *
     * <p>url: DELETE /monitor/cache/clearCacheName/{cacheName}</p>
     *
     * <p>底层先查 {@code KEYS cacheName*}，再批量 DEL，命中 key 数多时建议先运维评估。</p>
     *
     * @param url 请求路径
     * @param cacheName 缓存名称前缀
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheName/{cacheName}")
    public AjaxResult clearCacheName(@PathVariable String cacheName)
    {
        Collection<String> cacheKeys = redisTemplate.keys(cacheName + "*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }

    /**
     * 删除单个缓存 key
     *
     * <p>url: DELETE /monitor/cache/clearCacheKey/{cacheKey}</p>
     *
     * @param url 请求路径
     * @param cacheKey 完整 Redis key
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheKey/{cacheKey}")
    public AjaxResult clearCacheKey(@PathVariable String cacheKey)
    {
        redisTemplate.delete(cacheKey);
        return AjaxResult.success();
    }

    /**
     * 清空当前 Redis 库所有 key
     *
     * <p>url: DELETE /monitor/cache/clearCacheAll</p>
     *
     * <p>危险操作：先查 {@code KEYS *} 再批量 DEL，会影响全部业务缓存，生产慎用。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('monitor:cache:list')")
    @DeleteMapping("/clearCacheAll")
    public AjaxResult clearCacheAll()
    {
        Collection<String> cacheKeys = redisTemplate.keys("*");
        redisTemplate.delete(cacheKeys);
        return AjaxResult.success();
    }
}
