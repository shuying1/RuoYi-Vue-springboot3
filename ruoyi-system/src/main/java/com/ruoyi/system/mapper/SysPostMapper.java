package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysPost;

/**
 * 岗位信息表 sys_post 数据访问层
 *
 * 提供岗位信息的 CRUD 操作、唯一性校验（岗位名称和岗位编码），
 * 以及根据用户查询关联岗位、根据用户名查询岗位组等关联查询。
 * SQL 映射文件位于 resources/mapper/system/SysPostMapper.xml。
 *
 * @author ruoyi
 */
public interface SysPostMapper
{
    /**
     * 条件分页查询岗位列表
     * <p>支持按岗位名称模糊查询、岗位编码模糊查询、岗位状态筛选</p>
     *
     * @param post 岗位查询条件
     * @return 符合条件的岗位集合，无数据返回空列表
     */
    public List<SysPost> selectPostList(SysPost post);

    /**
     * 查询所有岗位（不带分页）
     * <p>通常用于下拉选择框的数据源</p>
     *
     * @return 所有岗位集合，无数据返回空列表
     */
    public List<SysPost> selectPostAll();

    /**
     * 根据岗位ID查询岗位详情
     *
     * @param postId 岗位主键ID
     * @return 岗位记录，未匹配到返回 null
     */
    public SysPost selectPostById(Long postId);

    /**
     * 根据用户ID查询该用户已分配的所有岗位ID
     * <p>用于用户编辑界面的岗位回显</p>
     *
     * @param userId 用户主键ID
     * @return 该用户关联的岗位ID列表，无数据返回空列表
     */
    public List<Long> selectPostListByUserId(Long userId);

    /**
     * 根据用户名查询该用户所属的岗位组
     * <p>通过联表 sys_user_post 查询，用于角色分配用户时的岗位展示</p>
     *
     * @param userName 用户登录账号
     * @return 该用户所属岗位集合，无数据返回空列表
     */
    public List<SysPost> selectPostsByUserName(String userName);

    /**
     * 删除单条岗位信息
     *
     * @param postId 待删除的岗位主键ID
     * @return 受影响的行数，成功删除返回 1
     */
    public int deletePostById(Long postId);

    /**
     * 批量删除岗位信息
     *
     * @param postIds 待删除的岗位主键ID数组
     * @return 受影响的行数总和
     */
    public int deletePostByIds(Long[] postIds);

    /**
     * 修改岗位信息
     *
     * @param post 岗位信息（需包含 postId 作为更新条件）
     * @return 受影响的行数，成功修改返回 1
     */
    public int updatePost(SysPost post);

    /**
     * 新增岗位信息
     *
     * @param post 岗位信息（需包含 postCode、postName、postSort）
     * @return 受影响的行数，成功新增返回 1
     */
    public int insertPost(SysPost post);

    /**
     * 根据岗位名称查询岗位（用于唯一性校验）
     *
     * @param postName 岗位名称
     * @return 匹配的岗位记录，未匹配到返回 null
     */
    public SysPost checkPostNameUnique(String postName);

    /**
     * 根据岗位编码查询岗位（用于唯一性校验）
     *
     * @param postCode 岗位编码
     * @return 匹配的岗位记录，未匹配到返回 null
     */
    public SysPost checkPostCodeUnique(String postCode);
}
