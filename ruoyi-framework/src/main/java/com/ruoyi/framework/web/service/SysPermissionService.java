package com.ruoyi.framework.web.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysMenuService;
import com.ruoyi.system.service.ISysRoleService;

/**
 * 用户权限服务 —— 角色 / 菜单权限数据聚合
 *
 * <p>设计意图：为 {@link UserDetailsServiceImpl} 提供权限数据加载能力，
 * 在登录时一次性查询出用户的所有角色和菜单权限字符串集合，
 * 打包到 {@link com.ruoyi.common.core.domain.model.LoginUser} 中存入 Redis，
 * 后续请求直接从缓存中读取，避免重复查库。</p>
 *
 * <p>权限模型（RBAC）：</p>
 * <ul>
 *   <li>管理员（admin）：拥有 {@link Constants#ALL_PERMISSION}（*:*:*）和 {@link Constants#SUPER_ADMIN}（admin）两个超级标识</li>
 *   <li>普通用户：通过用户 → 角色 → 菜单 的三层关联，聚合所有启用中角色的权限字符串</li>
 *   <li>每个角色在聚合权限时，会把该角色的 permissions 字段也填充好（给 {@link com.ruoyi.framework.aspectj.DataScopeAspect} 用于数据权限匹配）</li>
 * </ul>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link ISysRoleService} —— 查询用户关联的角色权限 Key</li>
 *   <li>{@link ISysMenuService} —— 查询角色/用户关联的菜单权限字符串</li>
 * </ul>
 *
 * @author ruoyi
 * @see UserDetailsServiceImpl 在认证链中调用本服务的两个方法构建 LoginUser
 * @see TokenService#refreshPermissionByRoleId 角色权限变更时调用本服务热刷新
 */
@Component
public class SysPermissionService
{
    /** 角色权限数据查询服务 */
    @Autowired
    private ISysRoleService roleService;

    /** 菜单权限字符串查询服务 */
    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取用户的角色权限 Key 集合。
     *
     * <p>管理员直接返回仅包含 {@link Constants#SUPER_ADMIN}（"admin"）的集合；
     * 普通用户从 sys_role / sys_user_role 关联查询。</p>
     *
     * <p>使用场景：Spring Security 的 {@link LoginUser#getAuthorities()} 中
     * 角色部分的数据来源。</p>
     *
     * @param user 当前登录用户（需包含 userId 和 admin 标志）
     * @return 角色权限 Key 集合（不可变 Set）
     */
    public Set<String> getRolePermission(SysUser user)
    {
        Set<String> roles = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            roles.add(Constants.SUPER_ADMIN);
        }
        else
        {
            roles.addAll(roleService.selectRolePermissionByUserId(user.getUserId()));
        }
        return roles;
    }

    /**
     * 获取用户的菜单权限字符串集合。
     *
     * <p>执行逻辑：</p>
     * <ol>
     *   <li>管理员直接返回仅包含 {@link Constants#ALL_PERMISSION}（"*:*:*"）的集合</li>
     *   <li>若用户已携带角色列表（user.getRoles() 非空，如从 Redis 反序列化），
     *       遍历每个启用中且非 admin 的角色，查询其菜单权限并聚合；
     *       同时将 permissions 字段回填到 Role 对象（给 DataScopeAspect 用）</li>
     *   <li>若用户未携带角色列表（仅 userId），直接调用 menuService.selectMenuPermsByUserId 查库</li>
     * </ol>
     *
     * <p>使用场景：构建 {@link LoginUser} 时填充 permissions 字段，
     * 以及角色权限变更热刷新（{@link TokenService#refreshPermissionByRoleId}）。</p>
     *
     * @param user 当前登录用户
     * @return 菜单权限字符串集合（如 ["system:user:list", "system:user:add", ...]）
     */
    public Set<String> getMenuPermission(SysUser user)
    {
        Set<String> perms = new HashSet<String>();
        // 管理员拥有所有权限
        if (user.isAdmin())
        {
            perms.add(Constants.ALL_PERMISSION);
        }
        else
        {
            List<SysRole> roles = user.getRoles();
            if (!CollectionUtils.isEmpty(roles))
            {
                // 多角色设置permissions属性，以便数据权限匹配权限
                for (SysRole role : roles)
                {
                    if (StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && !role.isAdmin())
                    {
                        Set<String> rolePerms = menuService.selectMenuPermsByRoleId(role.getRoleId());
                        role.setPermissions(rolePerms);
                        perms.addAll(rolePerms);
                    }
                }
            }
            else
            {
                perms.addAll(menuService.selectMenuPermsByUserId(user.getUserId()));
            }
        }
        return perms;
    }
}
