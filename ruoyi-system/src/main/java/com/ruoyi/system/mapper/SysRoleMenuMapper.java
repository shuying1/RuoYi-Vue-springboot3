package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysRoleMenu;

/**
 * 角色-菜单权限关联表 sys_role_menu 数据访问层
 *
 * 提供角色与功能权限（菜单）的关联维护操作：
 * 检查菜单是否被角色引用、根据角色删除关联、批量删除、批量新增。
 * 登录时根据用户所有角色查询此表汇总出功能权限集合。
 * SQL 映射文件位于 resources/mapper/system/SysRoleMenuMapper.xml。
 *
 * @author ruoyi
 */
public interface SysRoleMenuMapper
{
    /**
     * 查询指定菜单被多少个角色引用
     * <p>用于删除菜单前的引用检查，若大于 0 则不允许删除</p>
     *
     * @param menuId 菜单主键ID
     * @return 引用该菜单的角色数量
     */
    public int checkMenuExistRole(Long menuId);

    /**
     * 根据角色ID删除所有菜单关联
     * <p>在修改角色功能权限前，先清除旧关联</p>
     *
     * @param roleId 角色主键ID
     * @return 受影响的行数
     */
    public int deleteRoleMenuByRoleId(Long roleId);

    /**
     * 批量删除角色菜单关联记录
     *
     * @param ids 待删除的关联记录主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteRoleMenu(Long[] ids);

    /**
     * 批量新增角色-菜单关联关系
     * <p>在角色分配功能权限时，先 deleteRoleMenuByRoleId 再批量插入新关联</p>
     *
     * @param roleMenuList 角色菜单关联对象列表
     * @return 受影响的行数总和
     */
    public int batchRoleMenu(List<SysRoleMenu> roleMenuList);
}
