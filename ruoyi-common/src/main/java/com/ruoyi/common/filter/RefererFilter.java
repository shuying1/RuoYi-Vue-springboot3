package com.ruoyi.common.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 防盗链 Referer 校验过滤器
 *
 * <p>实现 Servlet Filter，从 HTTP 请求头中获取 Referer 字段，
 * 校验其是否包含在允许的域名白名单内。不在白名单中的请求（或 Referer 缺失）返回 403 Forbidden，
 * 用于防止第三方站点通过 img/iframe 标签非法引用本站资源（如图片、视频下载接口）。
 *
 * <p>在 web.xml 或 FilterRegistrationBean 中配置 allowedDomains 初始化参数，
 * 多个允许域名以英文逗号分隔。
 *
 * <p>技术栈：Jakarta Servlet
 *
 * @author ruoyi
 */
public class RefererFilter implements Filter
{
    /**
     * 允许的域名列表
     */
    public List<String> allowedDomains;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        String domains = filterConfig.getInitParameter("allowedDomains");
        this.allowedDomains = Arrays.asList(domains.split(","));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String referer = req.getHeader("Referer");

        // 如果Referer为空，拒绝访问
        if (referer == null || referer.isEmpty())
        {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Referer header is required");
            return;
        }

        // 检查Referer是否在允许的域名列表中
        boolean allowed = false;
        for (String domain : allowedDomains)
        {
            if (referer.contains(domain))
            {
                allowed = true;
                break;
            }
        }

        // 根据检查结果决定是否放行
        if (allowed)
        {
            chain.doFilter(request, response);
        }
        else
        {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Access denied: Referer '" + referer + "' is not allowed");
        }
    }

    @Override
    public void destroy()
    {

    }
}