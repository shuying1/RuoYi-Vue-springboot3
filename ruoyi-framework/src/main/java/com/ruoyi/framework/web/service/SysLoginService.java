package com.ruoyi.framework.web.service;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.exception.user.BlackListException;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.exception.user.UserNotExistsException;
import com.ruoyi.common.exception.user.UserPasswordNotMatchException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.framework.security.context.AuthenticationContextHolder;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 登录认证服务 —— 登录入口控制器
 *
 * <p>设计意图：封装完整的登录验证流程，从前端接收的用户名、密码、验证码开始，
 * 经过验证码校验 → 前置参数校验（密码长度、用户名长度、IP 黑名单）→ Spring Security 认证 →
 * 生成 JWT Token → 记录登录日志，最终返回 Token 给前端。</p>
 *
 * <p>登录执行流程：</p>
 * <ol>
 *   <li>{@link #validateCaptcha} —— 校验图形验证码（开关可配），从 Redis 对比，用后即删</li>
 *   <li>{@link #loginPreCheck} —— 前置校验：用户名/密码非空、长度范围、IP 黑名单</li>
 *   <li>{@code authenticationManager.authenticate()} —— 调用 Spring Security 认证链，
 *       内部会触发 {@link UserDetailsServiceImpl#loadUserByUsername} 加载用户，
 *       再由 {@link SysPasswordService#validate} 校验密码和重试次数</li>
 *   <li>将成功认证的 {@link LoginUser} 设置到 SecurityContext</li>
 *   <li>{@link #recordLoginInfo} —— 更新 sys_user 的登录 IP 和时间</li>
 *   <li>{@link TokenService#createToken} —— 生成 JWT 并返回</li>
 * </ol>
 *
 * <p>异常处理策略：所有登录失败场景都通过 {@link AsyncManager} 异步写入 sys_logininfor，
 * 然后抛出明确的业务异常（UserPasswordNotMatchException / CaptchaExpireException 等），
 * 由前端展示友好提示。</p>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link AuthenticationManager} —— Spring Security 核心认证接口</li>
 *   <li>{@link UserDetailsServiceImpl} —— 认证过程中自动被调用的用户加载服务</li>
 *   <li>{@link SysPasswordService} —— 密码校验 + 重试次数限制（在 AuthenticationContextHolder 中传递凭证）</li>
 *   <li>{@link TokenService} —— 登录成功后创建 JWT</li>
 *   <li>{@link ISysUserService} / {@link ISysConfigService} —— 用户信息查询、系统配置读取</li>
 * </ul>
 *
 * @author ruoyi
 * @see UserDetailsServiceImpl Spring Security 用户加载服务
 * @see SysPasswordService 密码校验与重试限制
 * @see TokenService JWT 创建与管理
 */
@Component
public class SysLoginService
{
    /** Token 服务，用于登录成功后创建 JWT */
    @Autowired
    private TokenService tokenService;

    /** Spring Security 认证管理器，核心认证入口 */
    @Resource
    private AuthenticationManager authenticationManager;

    /** Redis 缓存，用于验证码校验 */
    @Autowired
    private RedisCache redisCache;
    
    /** 用户数据库操作服务，用于更新登录信息 */
    @Autowired
    private ISysUserService userService;

    /** 系统配置读取服务（开关等） */
    @Autowired
    private ISysConfigService configService;

    /**
     * 完整登录验证入口 —— 对外暴露的登录 API 核心实现。
     *
     * <p>执行顺序：验证码校验 → 前置参数校验 → Spring Security 认证链 → 生成 Token → 记录登录信息。</p>
     *
     * <p>异常处理：</p>
     * <ul>
     *   <li>{@link BadCredentialsException} → 密码错误（记录日志 + 抛出 UserPasswordNotMatchException）</li>
     *   <li>其他 Exception → 包装为 ServiceException 直接抛出</li>
     * </ul>
     *
     * @param username 登录用户名
     * @param password 登录密码（前端加密前的原始值，BCryptPasswordEncoder 会处理）
     * @param code 验证码（从前端图形验证码获取）
     * @param uuid 验证码唯一标识（前端生成，用于从 Redis 中定位验证码）
     * @return 生成的 JWT Token 字符串，前端后续请求放在 Authorization Header 中
     * @throws CaptchaExpireException 验证码过期
     * @throws CaptchaException 验证码错误
     * @throws UserNotExistsException 用户名或密码为空
     * @throws UserPasswordNotMatchException 密码错误
     * @throws BlackListException IP 在黑名单中
     * @throws ServiceException 认证过程中其他业务异常
     */
    public String login(String username, String password, String code, String uuid)
    {
        // 验证码校验
        validateCaptcha(username, code, uuid);
        // 登录前置校验
        loginPreCheck(username, password);
        // 用户验证
        Authentication authentication = null;
        try
        {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            AuthenticationContextHolder.setContext(authenticationToken);
            // 该方法会去调用UserDetailsServiceImpl.loadUserByUsername
            authentication = authenticationManager.authenticate(authenticationToken);
        }
        catch (Exception e)
        {
            if (e instanceof BadCredentialsException)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
                throw new UserPasswordNotMatchException();
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, e.getMessage()));
                throw new ServiceException(e.getMessage());
            }
        }
        finally
        {
            AuthenticationContextHolder.clearContext();
        }
        AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success")));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        recordLoginInfo(loginUser.getUserId());
        // 生成token
        return tokenService.createToken(loginUser);
    }

    /**
     * 校验登录验证码。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>从 sys_config 表读取 {@code sys.account.captchaEnabled} 判断验证码是否开启</li>
     *   <li>从 Redis 读取验证码（Key = CAPTCHA_CODE_KEY + uuid）</li>
     *   <li>验证码不存在 → 过期异常；不匹配 → 错误异常</li>
     *   <li>无论验证成功与否，立即从 Redis 删除验证码（一次性使用，防重放）</li>
     * </ol>
     *
     * @param username 登录用户名（用于失败日志记录）
     * @param code 前端提交的验证码
     * @param uuid 验证码唯一标识
     * @throws CaptchaExpireException 验证码过期（Redis 中不存在）
     * @throws CaptchaException 验证码错误（与 Redis 中值不匹配）
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
            String captcha = redisCache.getCacheObject(verifyKey);
            if (captcha == null)
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.expire")));
                throw new CaptchaExpireException();
            }
            redisCache.deleteObject(verifyKey);
            if (!code.equalsIgnoreCase(captcha))
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.jcaptcha.error")));
                throw new CaptchaException();
            }
        }
    }

    /**
     * 登录前置校验 —— 参数合法性 + IP 黑名单检查。
     *
     * <p>校验项：</p>
     * <ul>
     *   <li>用户名 / 密码非空</li>
     *   <li>密码长度在 {@link UserConstants#PASSWORD_MIN_LENGTH} ~ {@link UserConstants#PASSWORD_MAX_LENGTH} 范围内</li>
     *   <li>用户名长度在 {@link UserConstants#USERNAME_MIN_LENGTH} ~ {@link UserConstants#USERNAME_MAX_LENGTH} 范围内</li>
     *   <li>当前 IP 不在 sys_config 配置的黑名单中</li>
     * </ul>
     *
     * <p>注意：此方法在 Spring Security 认证链之前执行，目的是快速拦截明显非法的登录请求，
     * 避免进入后续数据库查询开销。</p>
     *
     * @param username 登录用户名
     * @param password 登录密码
     * @throws UserNotExistsException 用户名或密码为空
     * @throws UserPasswordNotMatchException 用户名或密码长度不符合要求
     * @throws BlackListException IP 在黑名单中
     */
    public void loginPreCheck(String username, String password)
    {
        // 用户名或密码为空 错误
        if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("not.null")));
            throw new UserNotExistsException();
        }
        // 密码如果不在指定范围内 错误
        if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // 用户名不在指定范围内 错误
        if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("user.password.not.match")));
            throw new UserPasswordNotMatchException();
        }
        // IP黑名单校验
        String blackStr = configService.selectConfigByKey("sys.login.blackIPList");
        if (IpUtils.isMatchedIp(blackStr, IpUtils.getIpAddr()))
        {
            AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.LOGIN_FAIL, MessageUtils.message("login.blocked")));
            throw new BlackListException();
        }
    }

    /**
     * 登录成功后更新用户登录信息 —— 记录 IP 和最后登录时间。
     *
     * <p>写入 sys_user 表的 login_ip 和 login_date 字段。</p>
     *
     * @param userId 登录用户的 ID
     */
    public void recordLoginInfo(Long userId)
    {
        userService.updateLoginInfo(userId, IpUtils.getIpAddr(), DateUtils.getNowDate());
    }
}
