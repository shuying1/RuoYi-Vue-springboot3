package com.ruoyi.framework.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;
import com.ruoyi.framework.config.properties.PermitAllUrlProperties;
import com.ruoyi.framework.security.filter.JwtAuthenticationTokenFilter;
import com.ruoyi.framework.security.handle.AuthenticationEntryPointImpl;
import com.ruoyi.framework.security.handle.LogoutSuccessHandlerImpl;

/**
 * Spring Security 安全配置 —— JWT 无状态认证体系
 *
 * <p>设计意图：构建基于 Spring Security 的认证授权体系，核心特征：
 * 前后端分离、无状态（不使用 HttpSession）、JWT Token 认证、RBAC 权限模型。</p>
 *
 * <p>安全策略要点：</p>
 * <ol>
 *   <li>CSRF 禁用：无状态 JWT 架构不需要 CSRF 保护</li>
 *   <li>Session 管理：{@link SessionCreationPolicy#STATELESS}（不创建也不使用 HttpSession）</li>
 *   <li>认证入口：所有未认证请求返回 HTTP 401（由 {@link AuthenticationEntryPointImpl} 处理）</li>
 *   <li>登出：POST /logout 触发 {@link LogoutSuccessHandlerImpl} 清理 Redis 缓存</li>
 *   <li>密码编码器：BCrypt（强散列哈希，自适应 cost factor）</li>
 *   <li>权限模型：{@link EnableMethodSecurity} 开启 @PreAuthorize / @Secured 等注解</li>
 * </ol>
 *
 * <p>过滤器链顺序（由 {@link #filterChain} 定义）：</p>
 * <pre>{@code
 * CorsFilter → LogoutFilter → JwtAuthenticationTokenFilter → ... (Spring Security 默认过滤器链)
 * }</pre>
 *
 * <p>匿名访问 URL 规则：</p>
 * <ul>
 *   <li>所有标注了 {@link com.ruoyi.common.annotation.Anonymous} 注解的 Controller 方法 / 类</li>
 *   <li>/login、/register、/captchaImage（登录相关）</li>
 *   <li>静态资源（html/css/js/profile）、Swagger UI、Druid 监控</li>
 *   <li>其余所有请求均需认证（anyRequest().authenticated()）</li>
 * </ul>
 *
 * @author ruoyi
 * @see JwtAuthenticationTokenFilter JWT 认证过滤器
 * @see AuthenticationEntryPointImpl 认证失败 401 处理器
 * @see LogoutSuccessHandlerImpl 登出成功处理器
 * @see BCryptPasswordEncoder BCrypt 密码编码器
 */
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig
{
    /** 认证失败处理类 —— 返回 401 JSON 响应 */
    @Autowired
    private AuthenticationEntryPointImpl unauthorizedHandler;

    /** 退出处理类 —— 清理 Redis 登录缓存 + 异步记录登出日志 */
    @Autowired
    private LogoutSuccessHandlerImpl logoutSuccessHandler;

    /** JWT Token 认证过滤器 —— 从请求中提取 token 并注入 SecurityContext */
    @Autowired
    private JwtAuthenticationTokenFilter authenticationTokenFilter;
    
    /** 跨域过滤器 —— 解决前后端分离跨域问题 */
    @Autowired
    private CorsFilter corsFilter;

    /** 允许匿名访问的 URL 集合 —— 启动时扫描 {@link com.ruoyi.common.annotation.Anonymous} 注解生成 */
    @Autowired
    private PermitAllUrlProperties permitAllUrl;

	/**
	 * 暴露 Spring Security 的 AuthenticationManager Bean。
	 *
	 * <p>在 {@link com.ruoyi.framework.web.service.SysLoginService} 登录流程中，
	 * 通过该 Bean 调用 {@code authenticate()} 方法完成用户名密码校验。</p>
	 *
	 * @param authenticationConfiguration Spring 自动配置的认证配置对象
	 * @return Spring Security 的 AuthenticationManager 实例
	 * @throws Exception 获取 AuthenticationManager 时可能抛出的异常
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception 
	{
		return authenticationConfiguration.getAuthenticationManager();
	}

    /**
     * 构造 Spring Security 过滤器链。
     *
     * <p>安全配置策略详解：</p>
     * <ul>
     *   <li><b>CSRF 禁用</b>：JWT 无状态架构不依赖 Cookie，CSRF 攻击风险低</li>
     *   <li><b>HTTP Header</b>：Cache-Control 禁用（防止敏感资源缓存），X-Frame-Options 同源策略</li>
     *   <li><b>异常处理</b>：所有 AuthenticationException → 401 JSON</li>
     *   <li><b>Session 策略</b>：STATELESS，Spring Security 不创建 HttpSession</li>
     *   <li><b>URL 授权</b>：显式 permitAll + 其余 authenticated；Anonymous 注解扫描结果动态加入</li>
     *   <li><b>过滤器注入</b>：CorsFilter → JwtAuthenticationTokenFilter → UsernamePasswordAuthenticationFilter</li>
     * </ul>
     *
     * @param httpSecurity Spring Security 的 HttpSecurity 构建器
     * @return 构建好的 SecurityFilterChain Bean
     * @throws Exception 配置过程中的异常
     */
    @Bean
    protected SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception
    {
        return httpSecurity
            // CSRF禁用，因为不使用session（JWT 无状态架构）
            .csrf(csrf -> csrf.disable())
            // 禁用缓存控制头（避免敏感资源被浏览器缓存），设置 X-Frame-Options: SAMEORIGIN 防止点击劫持
            .headers((headersCustomizer) -> {
                headersCustomizer.cacheControl(cache -> cache.disable()).frameOptions(options -> options.sameOrigin());
            })
            // 认证失败处理类 —— 所有 AuthenticationException 被此处理器转为 401 JSON
            .exceptionHandling(exception -> exception.authenticationEntryPoint(unauthorizedHandler))
            // 基于 token，所以不需要 session（Spring Security 不会创建 HttpSession）
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 注解标记允许匿名访问的 URL —— 从 PermitAllUrlProperties 扫描 @Anonymous 注解自动加入
            .authorizeHttpRequests((requests) -> {
                permitAllUrl.getUrls().forEach(url -> requests.requestMatchers(url).permitAll());
                // 对于登录 login 注册 register 验证码 captchaImage 允许匿名访问
                requests.requestMatchers("/login", "/register", "/captchaImage").permitAll()
                    // 静态资源，可匿名访问（html/css/js 以及上传的 profile 目录）
                    .requestMatchers(HttpMethod.GET, "/", "/*.html", "/**.html", "/**.css", "/**.js", "/profile/**").permitAll()
                    // Swagger API 文档 + Druid 监控页面
                    .requestMatchers("/swagger-ui.html", "/v3/api-docs/**", "/swagger-ui/**", "/druid/**").permitAll()
                    // 除上面外的所有请求全部需要鉴权认证
                    .anyRequest().authenticated();
            })
            // 添加 Logout filter —— POST /logout 触发自定义登出处理器
            .logout(logout -> logout.logoutUrl("/logout").logoutSuccessHandler(logoutSuccessHandler))
            // 在 UsernamePasswordAuthenticationFilter 之前插入 JWT 过滤器（优先级高于表单登录）
            .addFilterBefore(authenticationTokenFilter, UsernamePasswordAuthenticationFilter.class)
            // CORS 过滤器放在 JWT 过滤器之前，确保跨域预检请求（OPTIONS）不会被 JWT 校验拦截
            .addFilterBefore(corsFilter, JwtAuthenticationTokenFilter.class)
            .addFilterBefore(corsFilter, LogoutFilter.class)
            .build();
    }

    /**
     * BCrypt 密码编码器 Bean。
     *
     * <p>用于用户注册时密码加密存储、登录时密码校验。
     * BCrypt 内置随机盐值和自适应 cost factor（默认 10），
     * 相比 MD5/SHA256 安全性更高，可抵御彩虹表和暴力破解。</p>
     *
     * @return BCryptPasswordEncoder 实例
     */
    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder()
    {
        return new BCryptPasswordEncoder();
    }
}
