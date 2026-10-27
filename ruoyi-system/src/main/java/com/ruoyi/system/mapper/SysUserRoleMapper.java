package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.SysUserRole;

/**
 * 用户-角色关联表 sys_user_role 数据访问层
 *
 * 提供 RBAC 权限模型中用户与角色的多对多关联维护操作：
 * 根据用户删除关联、批量删除、统计角色使用数量、批量新增、
 * 删除单条用户角色关联（用于取消授权）、批量取消授权。
 * SQL 映射文件位于 resources/mapper/system/SysUserRoleMapper.xml。
 *
 * @author ruoyi
 */
public interface SysUserRoleMapper
{
    /**
     * 根据用户ID删除所有角色关联
     * <p>在编辑用户角色前先清除旧关联</p>
     *
     * @param userId 用户主键ID
     * @return 受影响的行数
     */
    public int deleteUserRoleByUserId(Long userId);

    /**
     * 批量删除用户角色关联记录
     *
     * @param ids 待删除的关联记录主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteUserRole(Long[] ids);

    /**
     * 查询指定角色被多少个用户引用
     * <p>用于删除角色前的引用检查，若大于 0 则不允许删除</p>
     *
     * @param roleId 角色主键ID
     * @return 引用该角色的用户数量
     */
    public int countUserRoleByRoleId(Long roleId);

    /**
     * 批量新增用户-角色关联关系
     * <p>在用户分配角色时，先 deleteUserRoleByUserId 再批量插入新关联</p>
     *
     * @param userRoleList 用户角色关联对象列表
     * @return 受影响的行数总和
     */
    public int batchUserRole(List<SysUserRole> userRoleList);

    /**
     * 删除单条用户-角色关联（按具体 userId + roleId 定位）
     * <p>用于角色分配用户界面中取消某个用户的角色授权</p>
     *
     * @param userRole 用户-角色关联信息（需包含 userId 和 roleId）
     * @return 受影响的行数，成功删除返回 1
     */
    public int deleteUserRoleInfo(SysUserRole userRole);

    /**
     * 批量取消授权用户角色
     * <p>指定一个角色和多个用户，批量删除这些用户与该角色的关联</p>
     *
     * @param roleId  角色主键ID
     * @param userIds 需要取消授权的用户主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteUserRoleInfos(@Param("roleId") Long roleId, @Param("userIds") Long[] userIds);
}
