package com.ruoyi.common.enums;

import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;

/**
 * HTTP请求方法枚举
 * 
 * 标准HTTP方法枚举，提供从字符串方法名到枚举值的解析方法，
 * 以及判断方法是否匹配的便捷方法。
 * 
 * @author ruoyi
 */
public enum HttpMethod
{
    GET, HEAD, POST, PUT, PATCH, DELETE, OPTIONS, TRACE;

    /**
     * 方法名称到枚举值的映射表，用于快速查找
     */
    private static final Map<String, HttpMethod> mappings = new HashMap<>(16);

    /**
     * 类加载时一次性构建映射表
     */
    static
    {
        for (HttpMethod httpMethod : values())
        {
            mappings.put(httpMethod.name(), httpMethod);
        }
    }

    /**
     * 根据方法名字符串解析为HttpMethod枚举
     * 
     * @param method 方法名字符串，可为null
     * @return 对应的枚举值；找不到或输入为null时返回null
     */
    @Nullable
    public static HttpMethod resolve(@Nullable String method)
    {
        return (method != null ? mappings.get(method) : null);
    }

    /**
     * 判断当前枚举是否与指定方法名匹配
     * 
     * @param method 待比较的方法名字符串
     * @return true=方法名相同
     */
    public boolean matches(String method)
    {
        return (this == resolve(method));
    }
}
