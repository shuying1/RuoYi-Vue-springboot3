package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysUserPost;

/**
 * 用户-岗位关联表 sys_user_post 数据访问层
 *
 * 提供用户与岗位的多对多关联维护操作：
 * 根据用户删除关联、统计岗位使用数量、批量删除、批量新增。
 * 在用户新增/编辑时，先删除旧关联再批量插入新关联。
 * SQL 映射文件位于 resources/mapper/system/SysUserPostMapper.xml。
 *
 * @author ruoyi
 */
public interface SysUserPostMapper
{
    /**
     * 根据用户ID删除所有岗位关联
     * <p>在编辑用户岗位前先清除旧关联</p>
     *
     * @param userId 用户主键ID
     * @return 受影响的行数
     */
    public int deleteUserPostByUserId(Long userId);

    /**
     * 查询指定岗位被多少个用户引用
     * <p>用于删除岗位前的引用检查，若大于 0 则不允许删除</p>
     *
     * @param postId 岗位主键ID
     * @return 引用该岗位的用户数量
     */
    public int countUserPostById(Long postId);

    /**
     * 批量删除用户岗位关联记录
     *
     * @param ids 待删除的关联记录主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteUserPost(Long[] ids);

    /**
     * 批量新增用户-岗位关联关系
     * <p>在用户分配岗位时，先 deleteUserPostByUserId 再批量插入新关联</p>
     *
     * @param userPostList 用户岗位关联对象列表
     * @return 受影响的行数总和
     */
    public int batchUserPost(List<SysUserPost> userPostList);
}
