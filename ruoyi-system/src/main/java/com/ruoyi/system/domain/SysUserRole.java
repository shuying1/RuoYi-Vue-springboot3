package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户和角色关联 sys_user_role
 *
 * 用户-角色关联实体类，对应数据库表 sys_user_role。
 * 实现 RBAC 权限模型中用户与角色的多对多关联关系，一个用户可以拥有多个角色，
 * 登录时根据所有角色关联的菜单权限汇总出用户的完整功能权限集合。
 * 在用户新增/编辑/分配角色时批量写入，查询用户详情时用于回显已分配角色。
 * userId 和 roleId 联合构成业务唯一键。
 * 写入时机：SysUserServiceImpl.insertUser/updateUser 或 SysUserRoleMapper.batchUserRole。
 *
 * @author ruoyi
 */
public class SysUserRole
{
    /** 用户ID，关联 sys_user.user_id；与 roleId 联合唯一；不可空 */
    private Long userId;

    /** 角色ID，关联 sys_role.role_id；与 userId 联合唯一；不可空 */
    private Long roleId;

    /**
     * 获取用户ID
     *
     * @return 用户主键
     */
    public Long getUserId()
    {
        return userId;
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户主键
     */
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

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

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("roleId", getRoleId())
            .toString();
    }
}
