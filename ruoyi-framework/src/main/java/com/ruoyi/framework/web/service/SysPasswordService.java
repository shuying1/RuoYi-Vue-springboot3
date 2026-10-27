package com.ruoyi.framework.web.service;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.exception.user.UserPasswordRetryLimitExceedException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;

/**
 * 密码校验服务 —— 登录密码验证 + 登录失败重试限制
 *
 * <p>设计意图：在 Spring Security 认证链中（{@link UserDetailsServiceImpl#loadUserByUsername} 触发）
 * 执行实际的 BCrypt 密码匹配，并基于 Redis 计数器实现登录失败次数限制和账号临时锁定。</p>
 *
 * <p>调用链：</p>
 * <pre>{@code
 * SysLoginService.login → AuthenticationManager.authenticate
 *   → UserDetailsServiceImpl.loadUserByUsername
 *     → passwordService.validate(user)  // 本服务
 * }</pre>
 *
 * <p>工作流程（{@link #validate} 方法）：</p>
 * <ol>
 *   <li>从 {@link AuthenticationContextHolder} 取出 {@link UsernamePasswordAuthenticationToken}
 *       （因为 Spring Security 的 UserDetailsService 只接收 username，password 通过 ThreadLocal 传递）</li>
 *   <li>从 Redis 读取该账号当前的密码错误次数（Key = PWD_ERR_CNT_KEY + username）</li>
 *   <li>若次数 ≥ {@code user.password.maxRetryCount} → 抛出 UserPasswordRetryLimitExceedException（账号锁定）</li>
 *   <li>调用 {@link #matches} 执行 BCrypt 密码校验</li>
 *   <li>密码不匹配 → Redis 计数 +1 并设置 expireTime 为锁定时长，抛出 UserPasswordNotMatchException</li>
 *   <li>密码匹配 → 清除 Redis 中的错误计数，返回成功</li>
 * </ol>
 *
 * <p>关键配置：</p>
 * <ul>
 *   <li>{@code user.password.maxRetryCount} —— 最大允许密码错误次数（默认 5）</li>
 *   <li>{@code user.password.lockTime} —— 达到最大错误次数后锁定时长（分钟，默认 30）</li>
 * </ul>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link AuthenticationContextHolder} —— ThreadLocal 传递密码凭证</li>
 *   <li>{@link RedisCache} —— 密码错误计数存储</li>
 *   <li>{@link SecurityUtils#matchesPassword} —— BCrypt 密码匹配</li>
 * </ul>
 *
 * @author ruoyi
 * @see UserDetailsServiceImpl 在 Spring Security 认证链中调用本服务
 * @see AuthenticationContextHolder 跨线程方法传递密码的 ThreadLocal 容器
 */
@Component
public class SysPasswordService
{
    /** Redis 缓存操作，用于密码错误次数计数 */
    @Autowired
    private RedisCache redisCache;

    /** 最大允许密码错误次数（超过则临时锁定账号），从配置 {@code user.password.maxRetryCount} 读取 */
    @Value(value = "${user.password.maxRetryCount}")
    private int maxRetryCount;

    /** 账号临时锁定时长（分钟），从配置 {@code user.password.lockTime} 读取 */
    @Value(value = "${user.password.lockTime}")
    private int lockTime;

    /**
     * 构造 Redis 中密码错误次数的缓存 Key（内部方法）。
     *
     * <p>格式：{@code PwdErrCnt:}{username}</p>
     *
     * @param username 登录用户名
     * @return 完整的 Redis Key
     */
    private String getCacheKey(String username)
    {
        return CacheConstants.PWD_ERR_CNT_KEY + username;
    }

    /**
     * 密码校验主入口 —— Spring Security 认证链中被调用。
     *
     * <p>执行逻辑：读取 Redis 错误计数 → 判断是否超过最大重试次数 → BCrypt 密码匹配 → 更新计数或清零。</p>
     *
     * @param user 当前登录用户实体（由 {@link UserDetailsServiceImpl} 从数据库加载）
     * @throws UserPasswordRetryLimitExceedException 密码错误次数超限（账号锁定）
     * @throws UserPasswordNotMatchException 密码不匹配
     */
    public void validate(SysUser user)
    {
        Authentication usernamePasswordAuthenticationToken = AuthenticationContextHolder.getContext();
        String username = usernamePasswordAuthenticationToken.getName();
        String password = usernamePasswordAuthenticationToken.getCredentials().toString();

        Integer retryCount = redisCache.getCacheObject(getCacheKey(username));

        if (retryCount == null)
        {
            retryCount = 0;
        }

        if (retryCount >= Integer.valueOf(maxRetryCount).intValue())
        {
            throw new UserPasswordRetryLimitExceedException(maxRetryCount, lockTime);
        }

        if (!matches(user, password))
        {
            retryCount = retryCount + 1;
            redisCache.setCacheObject(getCacheKey(username), retryCount, lockTime, TimeUnit.MINUTES);
            throw new UserPasswordNotMatchException();
        }
        else
        {
            clearLoginRecordCache(username);
        }
    }

    /**
     * 执行 BCrypt 密码匹配。
     *
     * <p>委托给 {@link SecurityUtils#matchesPassword}，底层使用 BCryptPasswordEncoder.matches()，
     * 自动处理 BCrypt 内置的随机盐值和 cost factor。</p>
     *
     * @param user 数据库加载的用户实体（包含 BCrypt 加密后的密码）
     * @param rawPassword 前端提交的原始密码
     * @return true 表示密码匹配；false 表示密码错误
     */
    public boolean matches(SysUser user, String rawPassword)
    {
        return SecurityUtils.matchesPassword(rawPassword, user.getPassword());
    }

    /**
     * 密码验证通过后，清除 Redis 中该账号的密码错误次数缓存。
     *
     * <p>避免下次登录时累加旧计数。</p>
     *
     * @param loginName 登录用户名
     */
    public void clearLoginRecordCache(String loginName)
    {
        if (redisCache.hasKey(getCacheKey(loginName)))
        {
            redisCache.deleteObject(getCacheKey(loginName));
        }
    }
}
