package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysRoleDept;

/**
 * 角色-部门权限关联表 sys_role_dept 数据访问层
 *
 * 提供角色与数据权限范围（部门）的关联维护操作：
 * 根据角色删除关联、批量删除、统计部门使用数量、批量新增。
 * 该表在 @DataScope 数据权限切面中被查询，用于拼接 SQL 过滤条件。
 * SQL 映射文件位于 resources/mapper/system/SysRoleDeptMapper.xml。
 *
 * @author ruoyi
 */
public interface SysRoleDeptMapper
{
    /**
     * 根据角色ID删除所有部门关联
     * <p>在修改角色数据权限范围前，先清除旧关联</p>
     *
     * @param roleId 角色主键ID
     * @return 受影响的行数
     */
    public int deleteRoleDeptByRoleId(Long roleId);

    /**
     * 批量删除角色部门关联记录
     *
     * @param ids 待删除的关联记录主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteRoleDept(Long[] ids);

    /**
     * 查询指定部门被多少个角色引用
     * <p>用于删除部门前的引用检查，若大于 0 则不允许删除</p>
     *
     * @param deptId 部门主键ID
     * @return 引用该部门的角色数量
     */
    public int selectCountRoleDeptByDeptId(Long deptId);

    /**
     * 批量新增角色-部门关联关系
     * <p>在角色分配数据权限时，先 deleteRoleDeptByRoleId 再批量插入新关联</p>
     *
     * @param roleDeptList 角色部门关联对象列表
     * @return 受影响的行数总和
     */
    public int batchRoleDept(List<SysRoleDept> roleDeptList);
}
