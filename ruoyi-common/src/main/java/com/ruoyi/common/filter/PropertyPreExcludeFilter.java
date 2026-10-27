package com.ruoyi.common.filter;

import com.alibaba.fastjson2.filter.SimplePropertyPreFilter;

/**
 * FastJSON2 敏感属性前置排除过滤器
 *
 * <p>继承 {@link com.alibaba.fastjson2.filter.SimplePropertyPreFilter}，
 * 在序列化为 JSON 之前排除指定的敏感字段（如 password、token 等），
 * 防止敏感信息在 API 响应中意外泄露。
 *
 * <p>提供 addExcludes() 方法支持链式调用，方便在一个过滤器上累加多个排除字段。
 *
 * <p>典型用法：
 * <pre>{@code
 *   PropertyPreExcludeFilter excludeFilter = new PropertyPreExcludeFilter();
 *   excludeFilter.addExcludes("password", "secret");
 *   JSONObject.toJSONString(user, excludeFilter);
 * }</pre>
 *
 * <p>技术栈：FastJSON2
 *
 * @author ruoyi
 */
public class PropertyPreExcludeFilter extends SimplePropertyPreFilter
{
    public PropertyPreExcludeFilter()
    {
    }

    public PropertyPreExcludeFilter addExcludes(String... filters)
    {
        for (int i = 0; i < filters.length; i++)
        {
            this.getExcludes().add(filters[i]);
        }
        return this;
    }
}
