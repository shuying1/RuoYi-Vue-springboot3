package com.ruoyi.common.filter;

import java.io.IOException;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import com.ruoyi.common.utils.StringUtils;

/**
 * 可重复读取请求体过滤器
 *
 * <p>实现 Servlet Filter，拦截 Content-Type 为 application/json 的请求，
 * 将其包装为 {@link RepeatedlyRequestWrapper}。
 *
 * <p>Servlet 规范中 HttpServletRequest 的 InputStream 只能读取一次，
 * 一旦 Spring Security 或拦截器先读取了 Body，Controller 层就无法再反序列化 JSON。
 * 该过滤器通过提前将 JSON Body 缓存到 byte[] 中并提供重复读取能力，
 * 让后续任何环节都能按需读取请求体。
 *
 * <p>必须注册在 FilterRegistrationBean 顺序最前，且在所有需要读取 Body 的过滤器之前。
 *
 * <p>技术栈：Jakarta Servlet
 *
 * @author ruoyi
 */
public class RepeatableFilter implements Filter
{
    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        ServletRequest requestWrapper = null;
        if (request instanceof HttpServletRequest
                && StringUtils.startsWithIgnoreCase(request.getContentType(), MediaType.APPLICATION_JSON_VALUE))
        {
            requestWrapper = new RepeatedlyRequestWrapper((HttpServletRequest) request, response);
        }
        if (null == requestWrapper)
        {
            chain.doFilter(request, response);
        }
        else
        {
            chain.doFilter(requestWrapper, response);
        }
    }

    @Override
    public void destroy()
    {

    }
}
