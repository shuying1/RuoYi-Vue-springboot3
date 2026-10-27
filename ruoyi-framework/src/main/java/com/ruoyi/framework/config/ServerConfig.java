package com.ruoyi.framework.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.ServletUtils;

/**
 * 服务器地址配置 —— 获取应用对外可访问的 URL
 *
 * <p>设计意图：提供从当前请求上下文动态推导出服务基础地址的能力，
 * 用于生成完整的外部访问链接（如邮件验证链接、文件下载链接、回调地址等）。</p>
 *
 * <p>实现原理：从 {@link HttpServletRequest} 中获取 scheme://host:port + contextPath，
 * 拼接成形如 {@code http://192.168.1.100:8080/ruoyi} 的服务基础地址。</p>
 *
 * <p>注意：如果应用部署在反向代理（Nginx / Apache）之后，
 * 需要在代理层正确设置 X-Forwarded-* Header，
 * 否则 {@link ServletUtils} 可能获取到代理服务器的地址而非真实客户端地址。</p>
 *
 * @author ruoyi
 * @see ServletUtils#getRequest 获取当前请求上下文
 */
@Component
public class ServerConfig
{
    /**
     * 获取完整的服务基础地址（实例方法）。
     *
     * <p>内部调用静态方法 {@link #getDomain}。</p>
     *
     * @return 服务地址（如 http://localhost:8080/ruoyi）
     */
    public String getUrl()
    {
        HttpServletRequest request = ServletUtils.getRequest();
        return getDomain(request);
    }

    /**
     * 从 HttpServletRequest 推导出服务基础地址（静态方法）。
     *
     * <p>推导逻辑：</p>
     * <ol>
     *   <li>从 request.getRequestURL() 获取完整请求 URL
     *       （如 http://localhost:8080/ruoyi/system/user/list）</li>
     *   <li>用 delete() 去掉末尾的 request.getRequestURI()（/ruoyi/system/user/list）</li>
     *   <li>得到 http://localhost:8080</li>
     *   <li>追加 request.getContextPath()（如 /ruoyi）</li>
     *   <li>最终：http://localhost:8080/ruoyi</li>
     * </ol>
     *
     * @param request 当前 HTTP 请求
     * @return 拼接好的服务基础地址
     */
    public static String getDomain(HttpServletRequest request)
    {
        StringBuffer url = request.getRequestURL();
        String contextPath = request.getServletContext().getContextPath();
        return url.delete(url.length() - request.getRequestURI().length(), url.length()).append(contextPath).toString();
    }
}
