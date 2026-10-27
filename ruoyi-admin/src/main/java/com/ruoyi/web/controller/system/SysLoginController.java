package com.ruoyi.web.controller.system;

import java.util.Date;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginBody;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysLoginService;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysMenuService;

/**
 * 登录认证接口控制器
 * 
 * <p>负责 Spring Security JWT 认证流程的入口，同时暴露登录后前端所需的用户、角色、权限、菜单路由等信息。
 * 所有接口无需携带 token（{@code /login}、{@code /captchaImage} 等匿名访问路径已在 SecurityConfig 中放行）。</p>
 * 
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code POST /login}      — 提交账号密码登录，返回 accessToken</li>
 *   <li>{@code GET  /getInfo}    — 获取当前登录用户的角色、权限集合与密码策略配置</li>
 *   <li>{@code GET  /getRouters} — 获取当前用户有权限的菜单树（用于前端动态路由渲染）</li>
 * </ul>
 * 
 * @author ruoyi
 */
@RestController
public class SysLoginController
{
    /** 登录校验服务：账号/密码/验证码校验，生成 token */
    @Autowired
    private SysLoginService loginService;

    /** 菜单服务：用于根据当前用户权限构建前端路由 */
    @Autowired
    private ISysMenuService menuService;

    /** 权限服务：获取角色集、菜单权限集 */
    @Autowired
    private SysPermissionService permissionService;

    /** token 管理服务：负责 token 刷新 */
    @Autowired
    private TokenService tokenService;

    /** 系统参数配置服务：读取密码复杂度、密码过期等账户策略 */
    @Autowired
    private ISysConfigService configService;

    /**
     * 用户登录
     * 
     * <p>POST /login</p>
     * 
     * <p>请求体（JSON）：</p>
     * <pre>
     * {
     *   "username": "admin",          // 登录账号
     *   "password": "123456",         // 明文密码
     *   "code":     "a1b2",           // 验证码（sys.account.captchaEnabled=true 时必填）
     *   "uuid":     "xxxx-xxxx"       // 验证码 uuid，从 captchaImage 接口获取
     * }
     * </pre>
     * 
     * <p>成功响应：{@code {"code":200,"token":"xxx.xxx.xxx"}}</p>
     * 
     * @param loginBody 登录请求体，封装账号密码验证码等字段
     * @return AjaxResult 成功时携带 {@link Constants#TOKEN} 字段（JWT 令牌）
     */
    @PostMapping("/login")
    public AjaxResult login(@RequestBody LoginBody loginBody)
    {
        AjaxResult ajax = AjaxResult.success();
        // 生成令牌
        String token = loginService.login(loginBody.getUsername(), loginBody.getPassword(), loginBody.getCode(),
                loginBody.getUuid());
        ajax.put(Constants.TOKEN, token);
        return ajax;
    }

    /**
     * 获取当前登录用户信息
     * 
     * <p>GET /getInfo</p>
     * 
     * <p>响应 data 字段结构：</p>
     * <pre>
     * {
     *   "user":              SysUser,         // 用户基本信息（不含密码、盐等敏感字段）
     *   "roles":             ["admin"],       // 角色权限标识集合
     *   "permissions":       ["system:user:list", ...], // 按钮级权限标识集合
     *   "pwdChrtype":        "0",             // 密码复杂度配置：0=任意 1=字母+数字 2=字母+数字+特殊
     *   "isDefaultModifyPwd": true,           // 是否为首次登录需修改初始密码
     *   "isPasswordExpired": true             // 密码是否已过期（超出 passwordValidateDays 天数）
     * }
     * </pre>
     * 
     * <p>若当前用户的权限集与 token 中缓存的不一致（角色被变更），会同步刷新 token 中的权限缓存。</p>
     * 
     * @return AjaxResult 包含用户、角色、权限、密码策略信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("pwdChrtype", getSysAccountChrtype());
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        return ajax;
    }

    /**
     * 获取前端动态路由信息
     * 
     * <p>GET /getRouters</p>
     * 
     * <p>根据当前登录用户的 userId 查询其有权限的菜单（排除按钮类型），
     * 并通过 {@code menuService.buildMenus} 组装成 Vue Router 所需的树形结构。</p>
     * 
     * @return AjaxResult#success(List<RouterVo>) 路由树，每个节点包含 name、path、component、meta、children 等字段
     */
    @GetMapping("getRouters")
    public AjaxResult getRouters()
    {
        Long userId = SecurityUtils.getUserId();
        List<SysMenu> menus = menuService.selectMenuTreeByUserId(userId);
        return AjaxResult.success(menuService.buildMenus(menus));
    }

    /**
     * 获取密码复杂度配置值
     * <p>系统参数 {@code sys.account.chrtype}：0=不限制，1=字母+数字，2=字母+数字+特殊字符</p>
     * 
     * @return 密码复杂度标识，默认 "0"
     */
    public String getSysAccountChrtype()
    {
        return Convert.toStr(configService.selectConfigByKey("sys.account.chrtype"), "0");
    }

    /**
     * 判断是否为首次登录需修改初始密码
     * <p>条件：系统参数 {@code sys.account.initPasswordModify = 1} 且该用户从未修改过密码（pwdUpdateDate 为 null）</p>
     * 
     * @param pwdUpdateDate 用户最近一次修改密码的时间，为 null 说明未修改过
     * @return true 表示应在前端强制跳转到修改密码页
     */
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    /**
     * 判断用户密码是否已过期
     * <p>系统参数 {@code sys.account.passwordValidateDays} 配置密码有效期（天），0 或空表示不启用。
     * 若从未修改过初始密码也视为过期。</p>
     * 
     * @param pwdUpdateDate 用户最近一次修改密码的时间
     * @return true 表示密码已过期，需提醒用户修改
     */
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }
}
