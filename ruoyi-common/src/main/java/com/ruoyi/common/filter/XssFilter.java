package com.ruoyi.common.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.enums.HttpMethod;

/**
 * XSS 攻击拦截过滤器
 *
 * <p>实现 Servlet Filter 接口，在请求进入 Spring MVC 之前拦截并清理潜在的 XSS 恶意脚本。
 * 过滤器链中排在最前端，处理 POST/PUT 等方法请求，GET/DELETE 请求默认放行
 * （因为其请求参数通常由服务端渲染不受 XSS 影响）。
 *
 * <p>使用方式：在 web.xml 中注册或通过 WebMvcConfigurer 注入，配置 excludes 初始化参数
 * 指定跳过过滤的 URL（如 Swagger 文档、文件上传接口等）。
 *
 * <p>实际的 XSS 清理委托给 {@link XssHttpServletRequestWrapper} 和 {@link com.ruoyi.common.utils.html.EscapeUtil}
 *
 * <p>技术栈：Jakarta Servlet
 *
 * @author ruoyi
 */
public class XssFilter implements Filter
{
    /**
     * 排除链接
     */
    public List<String> excludes = new ArrayList<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException
    {
        String tempExcludes = filterConfig.getInitParameter("excludes");
        if (StringUtils.isNotEmpty(tempExcludes))
        {
            String[] urls = tempExcludes.split(",");
            for (String url : urls)
            {
                excludes.add(url);
            }
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException
    {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        if (handleExcludeURL(req, resp))
        {
            chain.doFilter(request, response);
            return;
        }
        XssHttpServletRequestWrapper xssRequest = new XssHttpServletRequestWrapper((HttpServletRequest) request);
        chain.doFilter(xssRequest, response);
    }

    private boolean handleExcludeURL(HttpServletRequest request, HttpServletResponse response)
    {
        String url = request.getServletPath();
        String method = request.getMethod();
        // GET DELETE 不过滤
        if (method == null || HttpMethod.GET.matches(method) || HttpMethod.DELETE.matches(method))
        {
            return true;
        }
        return StringUtils.matches(url, excludes);
    }

    @Override
    public void destroy()
    {

    }
}