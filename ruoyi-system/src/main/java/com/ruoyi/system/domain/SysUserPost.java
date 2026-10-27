package com.ruoyi.system.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 用户和岗位关联 sys_user_post
 *
 * 用户-岗位关联实体类，对应数据库表 sys_user_post。
 * 实现用户与岗位的多对多关联关系，一个用户可以拥有多个岗位，一个岗位可以分配给多个用户。
 * 在用户新增/编辑/分配岗位时批量写入，查询用户详情时用于回显已分配岗位。
 * userId 和 postId 联合构成业务唯一键。
 * 写入时机：SysUserServiceImpl.insertUser/updateUser 先删后插（userPostMapper.deleteUserPostByUserId → batchUserPost）。
 *
 * @author ruoyi
 */
public class SysUserPost
{
    /** 用户ID，关联 sys_user.user_id；与 postId 联合唯一；不可空 */
    private Long userId;

    /** 岗位ID，关联 sys_post.post_id；与 userId 联合唯一；不可空 */
    private Long postId;

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
     * 获取岗位ID
     *
     * @return 岗位主键
     */
    public Long getPostId()
    {
        return postId;
    }

    /**
     * 设置岗位ID
     *
     * @param postId 岗位主键
     */
    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("userId", getUserId())
            .append("postId", getPostId())
            .toString();
    }
}
