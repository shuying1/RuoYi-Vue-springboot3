package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 角色和部门关联 sys_role_dept
 *
 * 角色-部门数据权限关联实体类，对应数据库表 sys_role_dept。
 * 实现 RBAC 权限模型中角色与数据权限范围的关联，即某个角色可以查看/操作哪些部门的数据。
 * 该表在 @DataScope 数据权限切面中被查询，用于拼接 SQL 过滤条件。
 * roleId 和 deptId 联合构成业务唯一键。
 * 写入时机：SysRoleServiceImpl.authDataScope 在角色分配数据权限时先删后插。
 *
 * @author ruoyi
 */
public class SysRoleDept
{
    /** 角色ID，关联 sys_role.role_id；与 deptId 联合唯一；不可空 */
    private Long roleId;

    /** 部门ID，关联 sys_dept.dept_id；与 roleId 联合唯一；不可空 */
    private Long deptId;

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
     * 获取部门ID
     *
     * @return 部门主键
     */
    public Long getDeptId()
    {
        return deptId;
    }

    /**
     * 设置部门ID
     *
     * @param deptId 部门主键
     */
    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("roleId", getRoleId())
            .append("deptId", getDeptId())
            .toString();
    }
}
