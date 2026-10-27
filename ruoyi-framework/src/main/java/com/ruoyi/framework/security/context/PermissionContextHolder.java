package com.ruoyi.framework.security.context;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import com.ruoyi.common.core.text.Convert;

/**
 * 权限字符上下文持有者 —— 基于 RequestAttributes 的请求级存储
 *
 * <p>设计意图：在 {@link DataScopeAspect} 等切面中，
 * 当 {@link com.ruoyi.common.annotation.DataScope} 注解未显式指定 permission 时，
 * 从本类获取当前请求关联的权限字符，用于匹配用户角色的数据权限范围。</p>
 *
 * <p>实现原理：使用 {@link RequestContextHolder} 将权限字符串存入
 * {@link RequestAttributes#SCOPE_REQUEST}，保证在同一个 HTTP 请求内均可访问，
 * 请求结束后由 Spring 容器自动清理，无需手动 remove（与 ThreadLocal 不同）。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>在 {@link DataScopeAspect#handleDataScope} 中作为 DataScope 注解 permission 属性的回退值</li>
 *   <li>用于在 Controller → Service → Mapper 全链路中传递当前请求的权限标识</li>
 * </ul>
 *
 * @author ruoyi
 * @see DataScopeAspect 数据权限切面（本类的主要消费者）
 * @see RequestContextHolder Spring 请求上下文持有者
 */
public class PermissionContextHolder
{
    /** 权限字符在 RequestAttributes 中的 Attribute Key */
    private static final String PERMISSION_CONTEXT_ATTRIBUTES = "PERMISSION_CONTEXT";

    /**
     * 将当前请求关联的权限字符存入 RequestAttributes。
     *
     * @param permission 权限字符（如 "system:user:list"），会在后续数据权限过滤中用于匹配角色
     */
    public static void setContext(String permission)
    {
        RequestContextHolder.currentRequestAttributes().setAttribute(PERMISSION_CONTEXT_ATTRIBUTES, permission,
                RequestAttributes.SCOPE_REQUEST);
    }

    /**
     * 获取当前请求上下文绑定的权限字符。
     *
     * @return 权限字符；若未设置则返回空字符串（由 {@link Convert#toStr} 保证非 null）
     */
    public static String getContext()
    {
        return Convert.toStr(RequestContextHolder.currentRequestAttributes().getAttribute(PERMISSION_CONTEXT_ATTRIBUTES,
                RequestAttributes.SCOPE_REQUEST));
    }
}
