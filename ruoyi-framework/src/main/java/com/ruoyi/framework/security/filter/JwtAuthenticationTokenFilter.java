package com.ruoyi.framework.security.filter;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.TokenService;

/**
 * JWT 认证过滤器 —— 请求级 Token 验证与认证注入
 *
 * <p>设计意图：作为 Spring Security 过滤器链中的一环，
 * 对每个 HTTP 请求从 Header 中提取 Bearer Token，
 * 通过 {@link TokenService} 解析获取 {@link LoginUser}，
 * 并构造 {@link UsernamePasswordAuthenticationToken} 注入到
 * {@link SecurityContextHolder} 中，使后续的权限注解（@PreAuthorize）
 * 能直接从 SecurityContext 中拿到认证对象。</p>
 *
 * <p>JWT 验证流程：</p>
 * <ol>
 *   <li>继承 {@link OncePerRequestFilter}，保证每个请求只过滤一次（避免转发/包含时重复执行）</li>
 *   <li>调用 {@link TokenService#getLoginUser} 从请求头 Authorization 字段读取 token 并解析 Redis 中的登录信息</li>
 *   <li>若 loginUser 不为空且当前 SecurityContext 中尚未设置 Authentication，
 *       则调用 {@link TokenService#verifyToken} 校验 token 有效期和自动续期</li>
 *   <li>构造 {@link UsernamePasswordAuthenticationToken}（principal=loginUser, credentials=null, authorities=用户权限集合）</li>
 *   <li>设置 details 为 WebAuthenticationDetails（包含 remoteAddress、sessionId），便于后续审计</li>
 *   <li>将认证对象写入 {@link SecurityContextHolder}，放行到下一个过滤器</li>
 * </ol>
 *
 * <p>如果 Token 无效 / 过期 / 未登录，loginUser 为 null，
 * 过滤器不设置 Authentication，Spring Security 后续会触发 {@link AuthenticationEntryPointImpl} 返回 401。</p>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link TokenService} —— Token 解析与验证</li>
 *   <li>{@link SecurityUtils} —— 判断当前是否已存在认证对象，避免重复注入</li>
 * </ul>
 *
 * @author ruoyi
 * @see TokenService Token 解析/续期/验证服务
 * @see OncePerRequestFilter Spring 提供的一次性请求过滤基类
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter
{
    /** Token 服务，用于从请求中解析并验证 JWT */
    @Autowired
    private TokenService tokenService;

    /**
     * 过滤器核心逻辑 —— 完成 JWT 验证与 SecurityContext 注入。
     *
     * <p>处理流程：</p>
     * <ol>
     *   <li>从请求中解析 token → 得到 loginUser（含用户信息、权限、token 标识）</li>
     *   <li>若 loginUser 非空且 SecurityContext 为空 → 校验 token 并注入 Authentication</li>
     *   <li>无论是否注入成功，都调用 chain.doFilter() 放行到下一个过滤器</li>
     * </ol>
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param filterChain 过滤器链，用于继续传递请求
     * @throws ServletException Servlet 级异常
     * @throws IOException I/O 异常（如响应写入失败）
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        // 从请求 Authorization Header 中提取 token，并从 Redis 中查询对应的 LoginUser
        LoginUser loginUser = tokenService.getLoginUser(request);
        // 仅当 Token 有效且 SecurityContext 尚未注入认证对象时才执行（避免重复覆盖）
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNull(SecurityUtils.getAuthentication()))
        {
            // 校验 token 是否过期，并执行自动续期逻辑（距离过期不足 30 分钟时续期）
            tokenService.verifyToken(loginUser);
            // 构造 Spring Security 的认证令牌：principal=登录用户, credentials=null, authorities=权限集合
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
            // 设置认证详情（包含 remoteAddress、sessionId），便于审计和精确会话控制
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            // 将认证对象写入 SecurityContext，后续 Controller 中可通过 SecurityUtils.getLoginUser() 获取
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        }
        // 放行到下一个过滤器（无论是否注入认证对象）
        chain.doFilter(request, response);
    }
}
