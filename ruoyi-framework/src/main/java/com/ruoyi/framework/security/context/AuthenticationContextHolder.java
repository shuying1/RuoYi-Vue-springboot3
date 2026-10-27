package com.ruoyi.framework.security.context;

import org.springframework.security.core.Authentication;

/**
 * 自定义认证上下文持有者 —— 基于 ThreadLocal 的 Authentication 临时存储
 *
 * <p>设计意图：在某些特殊场景下（如 {@link com.ruoyi.framework.web.service.PermissionService}
 * 基于权限字符串进行切面预授权时），需要在 {@link SecurityContextHolder} 之外
 * 额外保存一份 {@link Authentication} 对象，以便在同一个请求线程中跨方法传递认证信息。</p>
 *
 * <p>与 {@link SecurityContextHolder} 的关系：</p>
 * <ul>
 *   <li>{@link SecurityContextHolder} 是 Spring Security 官方提供的全局认证上下文（基于 ThreadLocal）</li>
 *   <li>本类是对其的补充，主要用于 {@link com.ruoyi.framework.security.aspectj.PreAuthorizeAspect}
 *       等自定义切面在执行权限校验时的临时存储</li>
 * </ul>
 *
 * <p>线程安全：使用静态 {@link ThreadLocal}，每个线程拥有独立副本；
 * 必须在请求结束 / 线程池复用时调用 {@link #clearContext} 清理，防止内存泄漏。</p>
 *
 * @author ruoyi
 * @see SecurityContextHolder Spring Security 全局认证上下文
 * @see ThreadLocal 线程级变量存储
 */
public class AuthenticationContextHolder
{
    /** 线程级认证上下文存储，每个请求线程独立维护自己的 Authentication 副本 */
    private static final ThreadLocal<Authentication> contextHolder = new ThreadLocal<>();

    /**
     * 获取当前线程绑定的认证对象。
     *
     * @return 当前线程的 Authentication，若未设置则返回 null
     */
    public static Authentication getContext()
    {
        return contextHolder.get();
    }

    /**
     * 将认证对象绑定到当前线程。
     *
     * @param context 待绑定的 Authentication 实例
     */
    public static void setContext(Authentication context)
    {
        contextHolder.set(context);
    }

    /**
     * 清理当前线程的认证上下文，防止线程池复用时脏数据泄漏到后续请求。
     *
     * <p>通常在请求处理完成（或异常）后，在 finally 块中调用。</p>
     */
    public static void clearContext()
    {
        contextHolder.remove();
    }
}
