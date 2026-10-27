package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 角色和菜单关联 sys_role_menu
 *
 * 角色-菜单功能权限关联实体类，对应数据库表 sys_role_menu。
 * 实现 RBAC 权限模型中角色与功能权限的关联，即某个角色可以访问哪些菜单/接口。
 * 登录时根据用户所有角色查询此表，汇总得到用户的完整权限集合（Set<String>），
 * 用于接口权限校验（@PreAuthorize("@ss.hasPermi('...')")）和前端菜单渲染。
 * roleId 和 menuId 联合构成业务唯一键。
 * 写入时机：SysRoleServiceImpl.insertRole/updateRole 先删后插。
 *
 * @author ruoyi
 */
public class SysRoleMenu
{
    /** 角色ID，关联 sys_role.role_id；与 menuId 联合唯一；不可空 */
    private Long roleId;

    /** 菜单ID，关联 sys_menu.menu_id；与 roleId 联合唯一；不可空 */
    private Long menuId;

    /**
     * 获取角色ID
     *
     * @return 角色主键
     */
    public Long getRoleId()
    {
        return roleId;
    }

    /**
     * 设置角色ID
     *
     * @param roleId 角色主键
     */
    public void setRoleId(Long roleId)
    {
        this.roleId = roleId;
    }

    /**
     * 获取菜单ID
     *
     * @return 菜单主键
     */
    public Long getMenuId()
    {
        return menuId;
    }

    /**
     * 设置菜单ID
     *
     * @param menuId 菜单主键
     */
    public void setMenuId(Long menuId)
    {
        this.menuId = menuId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("roleId", getRoleId())
            .append("menuId", getMenuId())
            .toString();
    }
}
