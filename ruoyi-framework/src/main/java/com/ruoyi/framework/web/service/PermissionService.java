package com.ruoyi.framework.web.service;

import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.security.context.PermissionContextHolder;

/**
 * 自定义权限校验服务 —— 供 Thymeleaf 模板 / 自定义 @PreAuthorize 注解使用
 *
 * <p>设计意图：RuoYi 首创的权限方案。类上标注了 {@code @Service("ss")}，
 * 使得在 Thymeleaf 模板中可以通过 {@code ss.hasPermi('system:user:list')} 的写法
 * 或自定义的 {@code @PreAuthorize("@ss.hasPermi('xxx')")} 来进行细粒度权限控制。</p>
 *
 * <p>核心方法说明：</p>
 * <ul>
 *   <li>{@link #hasPermi} —— 单权限匹配：管理员或 permissions 包含指定字符串即通过</li>
 *   <li>{@link #hasAnyPermi} —— 任意权限匹配：使用 {@link Constants#PERMISSION_DELIMITER} 分隔多权限，任一命中即通过</li>
 *   <li>{@link #hasRole} —— 单角色匹配：管理员或角色列表包含指定角色 Key 即通过</li>
 *   <li>{@link #hasAnyRoles} —— 任意角色匹配：使用 {@link Constants#ROLE_DELIMITER} 分隔多角色</li>
 *   <li>{@link #lacksPermi} / {@link #lacksRole} —— 反向判断：不具备该权限/角色</li>
 * </ul>
 *
 * <p>权限集匹配算法（{@link #hasPermissions}）：</p>
 * <pre>{@code
 * permissions.contains(Constants.ALL_PERMISSION) || permissions.contains(permission)
 * }</pre>
 * 即只要拥有 "全部权限"（管理员）或直接包含目标权限字符串即通过。
 *
 * <p>与 {@link PermissionContextHolder} 的交互：
 * 在 hasPermi / hasAnyPermi 中会将当前校验的 permission 写入 PermissionContextHolder，
 * 供 {@link com.ruoyi.framework.aspectj.DataScopeAspect} 在同请求链路中获取，用于数据权限过滤时的角色匹配。</p>
 *
 * @author ruoyi
 * @see PermissionContextHolder 权限字符上下文（本服务写入，DataScopeAspect 读取）
 * @see LoginUser#getPermissions() 权限数据来源
 */
@Service("ss")
public class PermissionService
{
    /**
     * 验证当前登录用户是否具备指定权限字符串。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>权限字符串为空 → 返回 false</li>
     *   <li>SecurityUtils.getLoginUser() 为空或 permissions 为空 → 返回 false</li>
     *   <li>将 permission 写入 {@link PermissionContextHolder}（供数据权限切面使用）</li>
     *   <li>调用 {@link #hasPermissions} 匹配</li>
     * </ol>
     *
     * @param permission 权限字符串（如 "system:user:list"）
     * @return true 表示用户具备该权限；false 表示不具备
     */
    public boolean hasPermi(String permission)
    {
        if (StringUtils.isEmpty(permission))
        {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getPermissions()))
        {
            return false;
        }
        PermissionContextHolder.setContext(permission);
        return hasPermissions(loginUser.getPermissions(), permission);
    }

    /**
     * 验证用户是否 <b>不</b> 具备某权限。
     *
     * @param permission 权限字符串
     * @return true 表示用户确实不具备该权限（与 hasPermi 逻辑相反）
     */
    public boolean lacksPermi(String permission)
    {
        return hasPermi(permission) != true;
    }

    /**
     * 验证用户是否具备以下 <b>任意一个</b> 权限。
     *
     * <p>权限列表使用 {@link Constants#PERMISSION_DELIMITER}（逗号）分隔，
     * 只要其中一个权限命中即返回 true。</p>
     *
     * @param permissions 逗号分隔的权限列表（如 "system:user:list,system:role:list"）
     * @return true 表示用户至少具备其中一个权限
     */
    public boolean hasAnyPermi(String permissions)
    {
        if (StringUtils.isEmpty(permissions))
        {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getPermissions()))
        {
            return false;
        }
        PermissionContextHolder.setContext(permissions);
        Set<String> authorities = loginUser.getPermissions();
        for (String permission : permissions.split(Constants.PERMISSION_DELIMITER))
        {
            if (permission != null && hasPermissions(authorities, permission))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证当前登录用户是否拥有指定角色。
     *
     * <p>逻辑：</p>
     * <ol>
     *   <li>角色字符串为空 → 返回 false</li>
     *   <li>loginUser 或角色列表为空 → 返回 false</li>
     *   <li>遍历用户角色，管理员（admin）或角色 Key 匹配即通过</li>
     * </ol>
     *
     * @param role 角色权限 Key（如 "admin"、"common"）
     * @return true 表示用户具备该角色
     */
    public boolean hasRole(String role)
    {
        if (StringUtils.isEmpty(role))
        {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getUser().getRoles()))
        {
            return false;
        }
        for (SysRole sysRole : loginUser.getUser().getRoles())
        {
            String roleKey = sysRole.getRoleKey();
            if (Constants.SUPER_ADMIN.equals(roleKey) || roleKey.equals(StringUtils.trim(role)))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 验证用户是否 <b>不</b> 具备某角色。
     *
     * @param role 角色权限 Key
     * @return true 表示用户确实不具备该角色
     */
    public boolean lacksRole(String role)
    {
        return hasRole(role) != true;
    }

    /**
     * 验证用户是否具备以下 <b>任意一个</b> 角色。
     *
     * <p>角色列表使用 {@link Constants#ROLE_DELIMITER}（逗号）分隔。</p>
     *
     * @param roles 逗号分隔的角色列表（如 "admin,common"）
     * @return true 表示用户至少具备其中一个角色
     */
    public boolean hasAnyRoles(String roles)
    {
        if (StringUtils.isEmpty(roles))
        {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNull(loginUser) || CollectionUtils.isEmpty(loginUser.getUser().getRoles()))
        {
            return false;
        }
        for (String role : roles.split(Constants.ROLE_DELIMITER))
        {
            if (hasRole(role))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断权限集合中是否包含目标权限（内部方法）。
     *
     * <p>特殊处理：若权限集合中包含 {@link Constants#ALL_PERMISSION}（*:*:*，管理员），
     * 直接匹配所有权限请求。</p>
     *
     * @param permissions 用户的权限字符串集合
     * @param permission 待校验的权限字符串
     * @return true 表示匹配成功
     */
    private boolean hasPermissions(Set<String> permissions, String permission)
    {
        return permissions.contains(Constants.ALL_PERMISSION) || permissions.contains(StringUtils.trim(permission));
    }
}
