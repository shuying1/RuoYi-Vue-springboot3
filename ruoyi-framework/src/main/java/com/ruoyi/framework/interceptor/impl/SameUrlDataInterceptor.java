package com.ruoyi.framework.interceptor.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.filter.RepeatedlyRequestWrapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpHelper;
import com.ruoyi.framework.interceptor.RepeatSubmitInterceptor;

/**
 * 基于 URL + Body 参数比对的防重复提交实现
 *
 * <p>设计意图：继承 {@link RepeatSubmitInterceptor}，实现基于 Redis 的重复提交检测策略：
 * 同一 URL 在指定时间窗口内（默认 10 秒）若请求 Body 参数完全相同，
 * 则判定为重复提交，直接返回错误响应。</p>
 *
 * <p>防重提交判断流程（{@link #isRepeatSubmit}）：</p>
 * <ol>
 *   <li>尝试从 {@link RepeatedlyRequestWrapper}（由 {@link com.ruoyi.common.filter.RepeatableFilter} 包装）中读取 Body，
 *       若无法读取（如 GET 请求），则退化为读取 ParameterMap</li>
 *   <li>构造 nowDataMap = { repeatParams: 当前请求参数, repeatTime: 当前时间戳 }</li>
 *   <li>构造 Redis Key = REPEAT_SUBMIT_KEY + URL + Token Header（不同登录用户相互独立）</li>
 *   <li>若 Redis 中已有缓存 → 对比参数是否相同 + 时间间隔是否小于注解指定的 interval</li>
 *   <li>若相同且时间间隔不足 → 返回 true（重复提交）</li>
 *   <li>否则将 nowDataMap 写入 Redis，过期时间为 interval 毫秒，返回 false</li>
 * </ol>
 *
 * <p>为什么能读取 POST Body？
 * 因为 {@link com.ruoyi.framework.config.FilterConfig} 中的 RepeatableFilter
 * 提前将 HttpServletRequest 包装成 {@link RepeatedlyRequestWrapper}，
 * 使 Body 流可被多次读取。</p>
 *
 * @author ruoyi
 * @see RepeatSubmitInterceptor 防重提交抽象基类
 * @see com.ruoyi.common.filter.RepeatableFilter 使请求 Body 可被重复读取的 Filter
 * @see RepeatSubmit 防重提交注解（interval 字段可指定时间窗口）
 */
@Component
public class SameUrlDataInterceptor extends RepeatSubmitInterceptor
{
    /** nowDataMap / preDataMap 中存放参数内容的 Key */
    public final String REPEAT_PARAMS = "repeatParams";

    /** nowDataMap / preDataMap 中存放请求时间戳的 Key */
    public final String REPEAT_TIME = "repeatTime";

    /** 请求 Token Header 名称（从配置 {@code token.header} 读取），作为防重 Key 的一部分 */
    @Value("${token.header}")
    private String header;

    /** Redis 缓存操作 Bean，用于存储和比对历史请求参数 */
    @Autowired
    private RedisCache redisCache;

    /**
     * 具体的重复提交判断逻辑 —— 基于 URL + Body 参数哈希 + 时间窗口。
     *
     * <p>Redis 存储结构：{@code {url: {repeatParams: "...", repeatTime: 123456}}}</p>
     *
     * @param request 当前 HTTP 请求（需已被 RepeatableFilter 包装为 RepeatedlyRequestWrapper）
     * @param annotation 目标方法上的 {@link RepeatSubmit} 注解（interval = 防重时间窗口，单位毫秒）
     * @return true 表示是重复提交；false 表示是新的有效请求
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation)
    {
        String nowParams = "";
        if (request instanceof RepeatedlyRequestWrapper)
        {
            RepeatedlyRequestWrapper repeatedlyRequest = (RepeatedlyRequestWrapper) request;
            nowParams = HttpHelper.getBodyString(repeatedlyRequest);
        }

        // body参数为空，获取Parameter的数据
        if (StringUtils.isEmpty(nowParams))
        {
            nowParams = JSON.toJSONString(request.getParameterMap());
        }
        Map<String, Object> nowDataMap = new HashMap<String, Object>();
        nowDataMap.put(REPEAT_PARAMS, nowParams);
        nowDataMap.put(REPEAT_TIME, System.currentTimeMillis());

        // 请求地址（作为存放cache的key值）
        String url = request.getRequestURI();

        // 唯一值（没有消息头则使用请求地址）
        String submitKey = StringUtils.trimToEmpty(request.getHeader(header));

        // 唯一标识（指定key + url + 消息头）
        String cacheRepeatKey = CacheConstants.REPEAT_SUBMIT_KEY + url + submitKey;

        Object sessionObj = redisCache.getCacheObject(cacheRepeatKey);
        if (sessionObj != null)
        {
            Map<String, Object> sessionMap = (Map<String, Object>) sessionObj;
            if (sessionMap.containsKey(url))
            {
                Map<String, Object> preDataMap = (Map<String, Object>) sessionMap.get(url);
                if (compareParams(nowDataMap, preDataMap) && compareTime(nowDataMap, preDataMap, annotation.interval()))
                {
                    return true;
                }
            }
        }
        Map<String, Object> cacheMap = new HashMap<String, Object>();
        cacheMap.put(url, nowDataMap);
        redisCache.setCacheObject(cacheRepeatKey, cacheMap, annotation.interval(), TimeUnit.MILLISECONDS);
        return false;
    }

    /**
     * 比对两次请求的参数内容是否完全相同（内部方法）。
     *
     * <p>直接比较两个 JSON 字符串是否相等，
     * 只要有任何一个参数值不同就视为不同请求。</p>
     *
     * @param nowMap 当前请求的数据 Map
     * @param preMap 上一次请求的数据 Map（从 Redis 中读取）
     * @return true 表示参数完全相同
     */
    private boolean compareParams(Map<String, Object> nowMap, Map<String, Object> preMap)
    {
        String nowParams = (String) nowMap.get(REPEAT_PARAMS);
        String preParams = (String) preMap.get(REPEAT_PARAMS);
        return nowParams.equals(preParams);
    }

    /**
     * 判断两次请求的时间间隔是否小于注解指定的防重窗口（内部方法）。
     *
     * @param nowMap 当前请求的数据 Map
     * @param preMap 上一次请求的数据 Map
     * @param interval {@link RepeatSubmit#interval} 防重时间窗口（毫秒）
     * @return true 表示时间间隔不足（需要拦截为重复提交）
     */
    private boolean compareTime(Map<String, Object> nowMap, Map<String, Object> preMap, int interval)
    {
        long time1 = (Long) nowMap.get(REPEAT_TIME);
        long time2 = (Long) preMap.get(REPEAT_TIME);
        if ((time1 - time2) < interval)
        {
            return true;
        }
        return false;
    }
}
