package com.ruoyi.framework.security.handle;

import java.io.IOException;
import java.io.Serializable;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;

/**
 * 认证失败处理类 —— 返回统一 401 JSON 响应
 *
 * <p>设计意图：实现 Spring Security 的 {@link AuthenticationEntryPoint}，
 * 当匿名用户访问需要认证的资源（未登录 / Token 无效 / Token 过期）时，
 * 由 Spring Security 过滤器链末端触发此处理器，
 * 返回 HTTP 401 状态码 + {@link AjaxResult#error} 包装的 JSON 响应体，
 * 供前端识别未登录状态并自动跳转登录页。</p>
 *
 * <p>触发时机：</p>
 * <ul>
 *   <li>{@link JwtAuthenticationTokenFilter} 未设置 Authentication（token 为空 / 失效）</li>
 *   <li>Spring Security 的 ExceptionTranslationFilter 捕获到 AuthenticationException</li>
 * </ul>
 *
 * <p>与 {@link LogoutSuccessHandlerImpl} 配合使用：前者负责未认证拒绝，后者负责正常登出清理。</p>
 *
 * @author ruoyi
 * @see org.springframework.security.web.AuthenticationEntryPoint Spring Security 的认证入口点接口
 * @see ServletUtils#renderString(HttpServletResponse, String) JSON 响应渲染工具
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint, Serializable
{
    private static final long serialVersionUID = -8970718410437077606L;

    /**
     * 认证失败时的回调入口。
     *
     * <p>处理逻辑：构造包含当前请求 URI 的友好提示信息，
     * 使用 HTTP 401 状态码 + {@link AjaxResult#error} JSON 格式写入响应体。</p>
     *
     * @param request 触发认证失败的 HTTP 请求
     * @param response 将要写入 401 JSON 响应的 HTTP 响应对象
     * @param e Spring Security 抛出的认证异常（如 BadCredentialsException、InsufficientAuthenticationException 等）
     * @throws IOException 响应写入过程中的 I/O 异常
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e)
            throws IOException
    {
        // 返回 HTTP 401 状态码（未认证）
        int code = HttpStatus.UNAUTHORIZED;
        // 提示信息中包含请求 URI，便于前端调试定位未授权的具体接口
        String msg = StringUtils.format("请求访问：{}，认证失败，无法访问系统资源", request.getRequestURI());
        // 直接写入 JSON 响应体，不走 Spring MVC 的视图解析
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.error(code, msg)));
    }
}
