package com.ruoyi.framework.web.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.UserAgentUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;

/**
 * JWT Token 验证处理服务 —— 令牌生命周期管理核心
 *
 * <p>设计意图：基于 JWT（JSON Web Token）+ Redis 的混合认证方案，
 * 实现前后端分离架构下的无状态 Token 认证、自动续期、主动失效、权限热刷新等功能。</p>
 *
 * <p>JWT 认证模型（双重验证）：</p>
 * <ol>
 *   <li><b>JWT 本身</b> —— 使用 HS512 签名算法，Claims 中包含 LOGIN_USER_KEY（UUID）和 JWT_USERNAME，
 *       验证签名合法性判断 Token 是否被篡改</li>
 *   <li><b>Redis 缓存</b> —— 以 LOGIN_TOKEN_KEY + UUID 为 Key，存入完整的 LoginUser（含权限、IP、浏览器等），
 *       验证 UUID 存在性判断 Token 是否在服务端有效（支持主动登出）</li>
 * </ol>
 *
 * <p>Token 自动续期策略：</p>
 * <ul>
 *   <li>在 {@link JwtAuthenticationTokenFilter#doFilterInternal} 中，每次请求都会调用 {@link #verifyToken}</li>
 *   <li>若距离过期时间不足 20 分钟，自动刷新 Redis 中的 expireTime（滑动过期）</li>
 *   <li>这样只要用户持续活跃，Token 就不会过期；闲置超过配置时长则自动失效</li>
 * </ul>
 *
 * <p>在整体架构中的位置：</p>
 * <ul>
 *   <li>登录时由 {@link SysLoginService} 调用 {@link #createToken} 生成 JWT</li>
 *   <li>每个请求由 {@link JwtAuthenticationTokenFilter} 调用 {@link #getLoginUser} 解析</li>
 *   <li>登出时由 {@link com.ruoyi.framework.security.handle.LogoutSuccessHandlerImpl} 调用 {@link #delLoginUser} 失效</li>
 *   <li>角色权限变更时由 {@link #refreshPermissionByRoleId} 刷新所有在线用户的权限缓存</li>
 * </ul>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link RedisCache} —— 存储 LoginUser 的 Redis 操作</li>
 *   <li>JJWT 库（io.jsonwebtoken）—— JWT 创建与解析</li>
 *   <li>配置项：{@code token.header}（Header 名）、{@code token.secret}（签名密钥）、{@code token.expireTime}（有效期，分钟）</li>
 * </ul>
 *
 * @author ruoyi
 * @see JwtAuthenticationTokenFilter 本服务的主要消费者
 * @see SysLoginService 登录成功后调用 createToken 生成令牌
 * @see RedisCache 会话数据存储层
 */
@Component
public class TokenService
{
    /** Token 服务本地日志，记录 JWT 解析异常等问题 */
    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    /** 请求头中携带 Token 的 Header 名称，从配置 {@code token.header} 读取（默认 "Authorization"） */
    @Value("${token.header}")
    private String header;

    /** JWT 签名密钥，从配置 {@code token.secret} 读取，HS512 算法要求至少 64 字节 */
    @Value("${token.secret}")
    private String secret;

    /** Token 有效期（分钟），从配置 {@code token.expireTime} 读取（默认 30 分钟） */
    @Value("${token.expireTime}")
    private int expireTime;

    /** 每秒对应的毫秒数，用于时间单位换算 */
    protected static final long MILLIS_SECOND = 1000;

    /** 每分钟对应的毫秒数（60 × 1000），用于将 expireTime（分钟）转为毫秒 */
    protected static final long MILLIS_MINUTE = 60 * MILLIS_SECOND;

    /**
     * 自动续期阈值 —— 距离过期时间不足 20 分钟（20 × 60 × 1000 毫秒）时触发续期。
     * 目的：平衡安全性（Token 有最长闲置时间上限）与用户体验（持续活跃用户无感续期）。
     */
    private static final Long MILLIS_MINUTE_TWENTY = 20 * 60 * 1000L;

    /** Redis 缓存操作 Bean，用于存储/读取/删除登录用户信息 */
    @Autowired
    private RedisCache redisCache;

    /**
     * 从当前请求中解析并获取登录用户信息 —— JWT + Redis 双重验证的核心方法。
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>调用 {@link #getToken} 从请求 Header 中提取 Bearer Token</li>
     *   <li>调用 {@link #parseToken} 使用密钥解析 JWT Claims（验证签名合法性）</li>
     *   <li>从 Claims 中取出 LOGIN_USER_KEY（UUID），拼接成 Redis Key</li>
     *   <li>从 Redis 查询完整的 {@link LoginUser} 对象（验证 Token 是否在服务端有效）</li>
     * </ol>
     *
     * @param request 当前 HTTP 请求
     * @return 登录用户信息；若 Token 无效 / 过期 / Redis 中不存在则返回 null
     */
    public LoginUser getLoginUser(HttpServletRequest request)
    {
        // 获取请求携带的令牌
        String token = getToken(request);
        if (StringUtils.isNotEmpty(token))
        {
            try
            {
                Claims claims = parseToken(token);
                // 解析对应的权限以及用户信息
                String uuid = (String) claims.get(Constants.LOGIN_USER_KEY);
                String userKey = getTokenKey(uuid);
                LoginUser user = redisCache.getCacheObject(userKey);
                return user;
            }
            catch (Exception e)
            {
                log.error("获取用户信息异常'{}'", e.getMessage());
            }
        }
        return null;
    }

    /**
     * 将登录用户信息重新存入 Redis —— Token 续期或手动刷新权限时使用。
     *
     * <p>内部委托给 {@link #refreshToken} 执行实际的缓存写入。</p>
     *
     * @param loginUser 登录用户对象（需包含有效的 token 标识）
     */
    public void setLoginUser(LoginUser loginUser)
    {
        if (StringUtils.isNotNull(loginUser) && StringUtils.isNotEmpty(loginUser.getToken()))
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 删除指定 Token 对应的登录缓存 —— 登出 / 强制下线时调用。
     *
     * @param token 登录用户的唯一标识（UUID，对应 JWT Claims 中的 LOGIN_USER_KEY）
     */
    public void delLoginUser(String token)
    {
        if (StringUtils.isNotEmpty(token))
        {
            String userKey = getTokenKey(token);
            redisCache.deleteObject(userKey);
        }
    }

    /**
     * 创建 JWT Token —— 登录成功后生成令牌返回给前端。
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>生成唯一 UUID 作为 Token 标识（{@link IdUtils#fastUUID}）</li>
     *   <li>将 UUID 设置到 loginUser.token 字段，同时采集 User-Agent / IP / 地理位置</li>
     *   <li>将完整的 loginUser（含权限）写入 Redis，初始有效期 {@code expireTime} 分钟</li>
     *   <li>构造 JWT Claims：放入 LOGIN_USER_KEY（UUID）和 JWT_USERNAME（用户名）</li>
     *   <li>使用 HS512 签名算法 + {@code token.secret} 生成 JWT 字符串</li>
     * </ol>
     *
     * @param loginUser 登录用户信息（由 Spring Security AuthenticationManager 认证成功后的 Principal）
     * @return 生成的 JWT 字符串（形如 eyJhbGciOiJIUzUxMiJ9...）
     */
    public String createToken(LoginUser loginUser)
    {
        String token = IdUtils.fastUUID();
        loginUser.setToken(token);
        setUserAgent(loginUser);
        refreshToken(loginUser);

        Map<String, Object> claims = new HashMap<>();
        claims.put(Constants.LOGIN_USER_KEY, token);
        claims.put(Constants.JWT_USERNAME, loginUser.getUsername());
        return createToken(claims);
    }

    /**
     * 验证并自动续期 Token —— 滑动过期策略。
     *
     * <p>在 {@link JwtAuthenticationTokenFilter} 中每个请求都会调用此方法。
     * 若当前时间距离 loginUser.expireTime（Redis 中的过期时间）不足 20 分钟，
     * 则调用 {@link #refreshToken} 续期。这样只要用户持续活跃，Token 就不会过期。</p>
     *
     * <p>续期阈值：{@link #MILLIS_MINUTE_TWENTY} = 20 × 60 × 1000 毫秒</p>
     *
     * @param loginUser 当前登录用户（需包含 Redis 中的 expireTime 字段）
     */
    public void verifyToken(LoginUser loginUser)
    {
        long expireTime = loginUser.getExpireTime();
        long currentTime = System.currentTimeMillis();
        if (expireTime - currentTime <= MILLIS_MINUTE_TWENTY)
        {
            refreshToken(loginUser);
        }
    }

    /**
     * 刷新 Token 有效期 —— 更新 loginUser.loginTime / expireTime 并重写 Redis。
     *
     * <p>Redis 中的过期时间会被重置为 {@code expireTime} 分钟后（滑动窗口）。</p>
     *
     * @param loginUser 登录用户信息
     */
    public void refreshToken(LoginUser loginUser)
    {
        loginUser.setLoginTime(System.currentTimeMillis());
        loginUser.setExpireTime(loginUser.getLoginTime() + expireTime * MILLIS_MINUTE);
        // 根据uuid将loginUser缓存
        String userKey = getTokenKey(loginUser.getToken());
        redisCache.setCacheObject(userKey, loginUser, expireTime, TimeUnit.MINUTES);
    }

    /**
     * 设置登录用户的 User-Agent / IP / 地理位置 / 浏览器 / 操作系统信息。
     *
     * <p>采集的信息会存入 Redis 中的 LoginUser，用于登录日志记录和安全审计。</p>
     *
     * @param loginUser 登录用户对象（会被填充 ipaddr / loginLocation / browser / os 字段）
     */
    public void setUserAgent(LoginUser loginUser)
    {
        String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        String ip = IpUtils.getIpAddr();
        loginUser.setIpaddr(ip);
        loginUser.setLoginLocation(AddressUtils.getRealAddressByIP(ip));
        loginUser.setBrowser(UserAgentUtils.getBrowser(userAgent));
        loginUser.setOs(UserAgentUtils.getOperatingSystem(userAgent));
    }

    /**
     * 从 JWT Claims 生成令牌字符串（内部方法）。
     *
     * <p>使用 JJWT 库的 {@link SignatureAlgorithm#HS512} 算法 + {@code token.secret} 密钥进行签名。
     * HS512 是 HMAC-SHA512 对称签名，密钥长度需至少 64 字节。</p>
     *
     * @param claims JWT 载荷数据（包含 LOGIN_USER_KEY 和 JWT_USERNAME）
     * @return 完整的 JWT 字符串（三段式：Header.Payload.Signature）
     */
    private String createToken(Map<String, Object> claims)
    {
        String token = Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, secret).compact();
        return token;
    }

    /**
     * 解析 JWT 并获取 Claims（内部方法）。
     *
     * <p>会使用 {@code token.secret} 验证签名，若签名不匹配则抛出 SignatureException。</p>
     *
     * @param token 完整的 JWT 字符串
     * @return 解析后的 Claims 对象
     */
    private Claims parseToken(String token)
    {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 从 JWT 中获取用户名。
     *
     * <p>注意：当前 {@link #createToken(Map)} 中并未调用 {@code setSubject}，
     * 所以此方法可能返回 null。用户名应从 Claims 的 {@link Constants#JWT_USERNAME} 字段获取。</p>
     *
     * @param token 完整的 JWT 字符串
     * @return JWT Subject 字段（可能为 null）
     */
    public String getUsernameFromToken(String token)
    {
        Claims claims = parseToken(token);
        return claims.getSubject();
    }

    /**
     * 从 HTTP 请求中提取 Token（内部方法）。
     *
     * <p>查找流程：从配置指定的 Header（默认 Authorization）中读取值，
     * 若以 Bearer 前缀开头（{@link Constants#TOKEN_PREFIX}）则去掉前缀。</p>
     *
     * @param request 当前 HTTP 请求
     * @return 纯净的 Token 字符串；若 Header 不存在或为空则返回 null
     */
    private String getToken(HttpServletRequest request)
    {
        String token = request.getHeader(header);
        if (StringUtils.isNotEmpty(token) && token.startsWith(Constants.TOKEN_PREFIX))
        {
            token = token.replace(Constants.TOKEN_PREFIX, "");
        }
        return token;
    }

    /**
     * 构造 Redis 中登录用户缓存的 Key（内部方法）。
     *
     * <p>格式：{@code LoginTokens:cache_tokens:}{uuid}</p>
     *
     * @param uuid 登录用户的唯一标识 UUID
     * @return 完整的 Redis Key
     */
    private String getTokenKey(String uuid)
    {
        return CacheConstants.LOGIN_TOKEN_KEY + uuid;
    }

    /**
     * 角色权限变更后，刷新所有持有该角色的在线用户权限缓存。
     *
     * <p>使用场景：管理员修改某个角色的菜单权限后，需要立即让所有已登录且拥有该角色的用户
     * 感知到权限变更（不需要重新登录）。</p>
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>使用 {@code LoginTokens:cache_tokens:*} 模式扫描 Redis 中所有在线 Token Key</li>
     *   <li>逐个反序列化 LoginUser，判断该用户是否拥有变更的 roleId</li>
     *   <li>管理员（admin）直接跳过（拥有全部权限，无需刷新）</li>
     *   <li>对匹配用户调用 {@link SysPermissionService#getMenuPermission} 重新计算权限集合并刷新 Redis</li>
     * </ol>
     *
     * @param roleId 发生权限变更的角色 ID
     * @param permissionService 权限服务（用于重新计算菜单权限）
     */
    public void refreshPermissionByRoleId(Long roleId, SysPermissionService permissionService)
    {
        // 扫描所有在线 token
        String pattern = CacheConstants.LOGIN_TOKEN_KEY + "*";
        Collection<String> keys = redisCache.keys(pattern);
        if (keys == null || keys.isEmpty())
        {
            return;
        }
        for (String key : keys)
        {
            LoginUser loginUser = redisCache.getCacheObject(key);
            if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().isAdmin())
            {
                // 管理员拥有所有权限，跳过
                continue;
            }
            // 判断该用户是否拥有此角色
            boolean hasRole = loginUser.getUser().getRoles() != null
                    && loginUser.getUser().getRoles().stream().anyMatch(r -> roleId.equals(r.getRoleId()));
            if (!hasRole)
            {
                continue;
            }
            // 刷新权限缓存
            loginUser.setPermissions(permissionService.getMenuPermission(loginUser.getUser()));
            refreshToken(loginUser);
            log.info("角色[{}]权限变更，已刷新在线用户[{}]的权限缓存", roleId, loginUser.getUsername());
        }
    }
}
