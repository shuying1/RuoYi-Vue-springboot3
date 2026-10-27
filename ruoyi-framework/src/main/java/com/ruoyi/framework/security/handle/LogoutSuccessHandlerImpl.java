package com.ruoyi.framework.security.handle;

import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.web.service.TokenService;

/**
 * 自定义登出成功处理器
 *
 * <p>设计意图：实现 Spring Security 的 {@link LogoutSuccessHandler}，
 * 在用户主动调用登出接口（通常是 POST /logout）时执行，
 * 完成 Redis 中用户登录缓存的清理、退出日志的异步记录，
 * 并向前端返回统一的 JSON 成功响应。</p>
 *
 * <p>登出流程：</p>
 * <ol>
 *   <li>通过 {@link TokenService} 从请求中获取当前登录用户（loginUser）</li>
 *   <li>若 loginUser 不为空 → 调用 {@link TokenService#delLoginUser} 删除 Redis 中的登录缓存（包含 token 标识和用户信息）</li>
 *   <li>通过 {@link AsyncManager} 异步写入 sys_logininfor 登出日志，避免阻塞响应</li>
 *   <li>返回 AjaxResult.success + 用户登出成功的国际化提示</li>
 * </ol>
 *
 * <p>与 {@link AuthenticationEntryPointImpl} 的区别：本类处理"主动退出"的成功响应；
 * 后者处理"未登录访问"的 401 拒绝。</p>
 *
 * @author ruoyi
 * @see LogoutSuccessHandler Spring Security 登出成功回调接口
 * @see TokenService#delLoginUser(String) 删除 Redis 中的登录缓存
 */
@Configuration
public class LogoutSuccessHandlerImpl implements LogoutSuccessHandler
{
    /** Token 服务，用于登出时清理 Redis 中的登录缓存 */
    @Autowired
    private TokenService tokenService;

    /**
     * 用户主动登出后的回调处理。
     *
     * <p>主要工作：</p>
     * <ul>
     *   <li>解析当前请求对应的登录用户</li>
     *   <li>删除 Redis 中的登录缓存（使该 token 立即失效）</li>
     *   <li>异步记录登出日志</li>
     *   <li>返回统一的 JSON 成功响应</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param authentication 当前已认证的 Authentication 对象（部分场景可能为 null，如 token 已失效）
     * @throws IOException 响应写入过程中的 I/O 异常
     * @throws ServletException Servlet 级异常
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException
    {
        LoginUser loginUser = tokenService.getLoginUser(request);
        if (StringUtils.isNotNull(loginUser))
        {
            String userName = loginUser.getUsername();
            // 删除 Redis 中该用户的登录缓存（token 立即失效，防止被盗用继续使用）
            tokenService.delLoginUser(loginUser.getToken());
            // 异步记录用户登出日志（写入 sys_logininfor 表）
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(userName, Constants.LOGOUT, MessageUtils.message("user.logout.success")));
        }
        // 返回统一 JSON 成功响应，前端据此清除本地 token 并跳转登录页
        ServletUtils.renderString(response, JSON.toJSONString(AjaxResult.success(MessageUtils.message("user.logout.success"))));
    }
}
