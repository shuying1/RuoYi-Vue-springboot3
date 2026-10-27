package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.bean.BeanValidators;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.SysPost;
import com.ruoyi.system.domain.SysUserPost;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.mapper.SysPostMapper;
import com.ruoyi.system.mapper.SysRoleMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.mapper.SysUserPostMapper;
import com.ruoyi.system.mapper.SysUserRoleMapper;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 用户管理 业务层实现
 *
 * 实现 ISysUserService 接口，负责系统用户的 CRUD、唯一性校验、数据权限校验、
 * 用户-角色/岗位关联维护、密码重置、Excel 批量导入等核心业务。
 * 查询类方法使用 @DataScope(deptAlias="d", userAlias="u") 数据权限切面，
 * 根据当前登录用户的数据权限范围自动拼接 SQL 过滤条件。
 * insertUser/updateUser/insertUserAuth/deleteUserById/deleteUserByIds 使用 @Transactional 事务注解，
 * 保证主表和关联表（sys_user_role、sys_user_post）的原子性一致性。
 *
 * 业务规则速览：
 * - checkUserAllowed：禁止对超级管理员（userId=1 且 admin=true）进行任何修改/删除/状态变更操作
 * - checkUserDataScope：非超级管理员只能操作自己数据权限范围内的用户
 * - importUser：Excel 批量导入，支持"已存在则更新"开关；需通过部门数据权限校验
 * - 新增用户时密码使用 sys.user.initPassword 参数从 Redis/DB 获取并加密
 *
 * @author ruoyi
 */
@Service
public class SysUserServiceImpl implements ISysUserService
{
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPostMapper postMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysUserPostMapper userPostMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    protected Validator validator;

    /**
     * 根据条件分页查询用户列表
     *
     * <p>数据权限切面 @DataScope(deptAlias="d", userAlias="u") 触发时机：</p>
     * <p>在进入本方法体之前，DataScopeAspect 会拦截并向 SysUserMapper.selectUserList 的 SQL 参数中注入
     * 数据权限过滤字段（dataScope 字符串），非超级管理员只能查询自己数据权限范围内（deptId 在可操作部门集合中）的用户。</p>
     *
     * @param user 包含查询条件的用户对象（userName、status、deptId 等非空字段作为过滤条件）
     * @return 符合条件的用户列表（已应用数据权限过滤）
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUserList(SysUser user)
    {
        return userMapper.selectUserList(user);
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectAllocatedList(SysUser user)
    {
        return userMapper.selectAllocatedList(user);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUnallocatedList(SysUser user)
    {
        return userMapper.selectUnallocatedList(user);
    }

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByUserName(String userName)
    {
        return userMapper.selectUserByUserName(userName);
    }

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserById(Long userId)
    {
        return userMapper.selectUserById(userId);
    }

    /**
     * 查询用户所属角色组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName)
    {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName)
    {
        List<SysPost> list = postMapper.selectPostsByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(SysPost::getPostName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkUserNameUnique(user.getUserName());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验用户是否允许操作
     *
     * <p>业务规则：禁止对超级管理员用户（userId 非空且 isAdmin()=true，即 userId=1）进行任何修改/删除操作。
     * 超级管理员由 Constants.SUPER_ADMIN_ID 标识，不受数据权限限制。</p>
     *
     * @param user 待校验的用户（需包含 userId 或 admin 标识）
     * @throws ServiceException 当操作超级管理员时抛出
     */
    @Override
    public void checkUserAllowed(SysUser user)
    {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     *
     * <p>校验逻辑：</p>
     * <ol>
     *   <li>超级管理员跳过校验（SecurityUtils.isAdmin()=true）</li>
     *   <li>非超级管理员：通过 Spring AOP 代理调用 selectUserList（触发 @DataScope 切面），
     *       传入目标 userId 作为条件，若返回列表为空说明该用户不在当前登录人的数据权限范围内，拒绝操作</li>
     * </ol>
     * <p>此处必须使用 SpringUtils.getAopProxy(this) 调用，否则同类内部调用不会触发 AOP 切面。</p>
     *
     * @param userId 待操作的目标用户ID
     * @throws ServiceException 当无数据权限时抛出
     */
    @Override
    public void checkUserDataScope(Long userId)
    {
        if (!SecurityUtils.isAdmin())
        {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = SpringUtils.getAopProxy(this).selectUserList(user);
            if (StringUtils.isEmpty(users))
            {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 新增保存用户信息
     *
     * <p>事务边界 @Transactional：覆盖主表 sys_user 插入 + sys_user_role/sys_user_post 关联表批量插入，
     * 任何一步失败都会整体回滚。</p>
     *
     * <p>执行顺序：</p>
     * <ol>
     *   <li>userMapper.insertUser(user) — 插入主表，此时 userId 已由 MyBatis 回填</li>
     *   <li>insertUserPost(user) — 使用回填后的 userId 批量插入岗位关联</li>
     *   <li>insertUserRole(user) — 使用回填后的 userId 批量插入角色关联</li>
     * </ol>
     *
     * @param user 用户信息（需包含 userName、password、deptId、roleIds、postIds 等）
     * @return 受影响的行数
     */
    @Override
    @Transactional
    public int insertUser(SysUser user)
    {
        // 新增用户信息
        int rows = userMapper.insertUser(user);
        // 新增用户岗位关联
        insertUserPost(user);
        // 新增用户与角色管理
        insertUserRole(user);
        return rows;
    }

    /**
     * 注册用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user)
    {
        return userMapper.insertUser(user) > 0;
    }

    /**
     * 修改保存用户信息
     *
     * <p>事务边界 @Transactional：采用"先删后插"策略维护用户-角色/岗位关联，
     * 确保主表更新与关联表重建的原子性。</p>
     *
     * <p>执行顺序：</p>
     * <ol>
     *   <li>删除旧的用户-角色关联（deleteUserRoleByUserId）</li>
     *   <li>批量插入新的用户-角色关联（insertUserRole）</li>
     *   <li>删除旧的用户-岗位关联（deleteUserPostByUserId）</li>
     *   <li>批量插入新的用户-岗位关联（insertUserPost）</li>
     *   <li>更新用户主表信息（userMapper.updateUser）</li>
     * </ol>
     *
     * @param user 用户信息（需包含 userId 作为更新条件，roleIds/postIds 为关联的新值）
     * @return 主表更新受影响的行数
     */
    @Override
    @Transactional
    public int updateUser(SysUser user)
    {
        Long userId = user.getUserId();
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 新增用户与角色管理
        insertUserRole(user);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPostByUserId(userId);
        // 新增用户与岗位管理
        insertUserPost(user);
        return userMapper.updateUser(user);
    }

    /**
     * 用户授权角色
     *
     * <p>事务边界 @Transactional：用于角色分配用户界面中单独修改某个用户的角色列表，
     * 只涉及 sys_user_role 关联表的重建，不触碰主表和岗位关联。</p>
     * <p>执行策略：先删除该用户所有旧角色关联 → 批量插入新的角色关联。</p>
     *
     * @param userId  待授权的用户主键
     * @param roleIds 新的角色ID数组（null 或空数组则相当于清空该用户的所有角色）
     */
    @Override
    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user)
    {
        return userMapper.updateUserStatus(user.getUserId(), user.getStatus());
    }

    /**
     * 修改用户基本信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户头像
     * 
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(Long userId, String avatar)
    {
        return userMapper.updateUserAvatar(userId, avatar) > 0;
    }

    /**
     * 更新用户登录信息（IP和登录时间）
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginDate 登录时间
     * @return 结果
     */
    public void updateLoginInfo(Long userId, String loginIp, Date loginDate)
    {
        userMapper.updateLoginInfo(userId, loginIp, loginDate);
    }

    /**
     * 重置用户密码
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user)
    {
        return userMapper.resetUserPwd(user.getUserId(), user.getPassword());
    }

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password)
    {
        return userMapper.resetUserPwd(userId, password);
    }

    /**
     * 新增用户角色信息
     * 
     * @param user 用户对象
     */
    public void insertUserRole(SysUser user)
    {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * 新增用户岗位信息
     * 
     * @param user 用户对象
     */
    public void insertUserPost(SysUser user)
    {
        Long[] posts = user.getPostIds();
        if (StringUtils.isNotEmpty(posts))
        {
            // 新增用户与岗位管理
            List<SysUserPost> list = new ArrayList<SysUserPost>(posts.length);
            for (Long postId : posts)
            {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                list.add(up);
            }
            userPostMapper.batchUserPost(list);
        }
    }

    /**
     * 新增用户角色信息
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds)
    {
        if (StringUtils.isNotEmpty(roleIds))
        {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>(roleIds.length);
            for (Long roleId : roleIds)
            {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.batchUserRole(list);
        }
    }

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserById(Long userId)
    {
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 删除用户与岗位表
        userPostMapper.deleteUserPostByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    /**
     * 批量删除用户信息
     *
     * <p>事务边界 @Transactional：对每个 userId 循环执行校验 + 关联清理 + 主表删除。</p>
     * <p>前置校验：</p>
     * <ul>
     *   <li>checkUserAllowed — 禁止删除超级管理员</li>
     *   <li>checkUserDataScope — 非超级管理员只能删除自己数据权限范围内的用户</li>
     * </ul>
     * <p>执行顺序（对每个 userId）：先删关联表 → 再删主表，整体事务保证一致性。</p>
     *
     * @param userIds 待删除的用户主键数组
     * @return 主表批量删除受影响的行数
     */
    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        // 删除用户与角色关联
        userRoleMapper.deleteUserRole(userIds);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPost(userIds);
        return userMapper.deleteUserByIds(userIds);
    }

    /**
     * 导入用户数据（Excel 批量导入）
     *
     * <p>业务规则：</p>
     * <ol>
     *   <li>空列表直接抛出 ServiceException</li>
     *   <li>逐条处理，每条独立 try/catch，单条失败不影响其他条目，最终合并成功/失败消息</li>
     *   <li>用户不存在 → 新增流程：
     *       <ul>
     *         <li>BeanValidators 校验 Excel 字段合法性</li>
     *         <li>deptService.checkDeptDataScope — 当前登录人必须对用户所属部门有数据权限</li>
     *         <li>从参数表读取 sys.user.initPassword 并加密作为初始密码</li>
     *       </ul>
     *   </li>
     *   <li>用户已存在 + isUpdateSupport=true → 更新流程：
     *       <ul>
     *         <li>checkUserAllowed/checkUserDataScope — 禁止修改超级管理员和越权用户</li>
     *         <li>强制保留原 userId 和 deptId（以已有记录为准，防止越权修改部门）</li>
     *       </ul>
     *   </li>
     *   <li>用户已存在 + isUpdateSupport=false → 失败记录</li>
     *   <li>有失败条目 → 抛出 ServiceException（包含所有成功/失败明细）</li>
     * </ol>
     *
     * <p>注意：本方法未加 @Transactional，因为每条独立成功即落库，整体是"尽力而为"语义。</p>
     *
     * @param userList        从 Excel 解析的用户列表
     * @param isUpdateSupport 是否已存在时是否执行更新（true=更新，false=跳过）
     * @param operName        执行导入操作的登录账号，写入 createBy/updateBy
     * @return 成功时返回拼接的成功消息（HTML 格式），失败时抛 ServiceException
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysUser user : userList)
        {
            try
            {
                // 验证是否存在这个用户
                SysUser u = userMapper.selectUserByUserName(user.getUserName());
                if (StringUtils.isNull(u))
                {
                    BeanValidators.validateWithException(validator, user);
                    deptService.checkDeptDataScope(user.getDeptId());
                    String password = configService.selectConfigByKey("sys.user.initPassword");
                    user.setPassword(SecurityUtils.encryptPassword(password));
                    user.setCreateBy(operName);
                    userMapper.insertUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 导入成功");
                }
                else if (isUpdateSupport)
                {
                    BeanValidators.validateWithException(validator, user);
                    checkUserAllowed(u);
                    checkUserDataScope(u.getUserId());
                    deptService.checkDeptDataScope(user.getDeptId());
                    user.setUserId(u.getUserId());
                    user.setDeptId(u.getDeptId());
                    user.setUpdateBy(operName);
                    userMapper.updateUser(user);
                    successNum++;
                    successMsg.append("<br/>" + successNum + "、账号 " + user.getUserName() + " 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>" + failureNum + "、账号 " + user.getUserName() + " 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg + e.getMessage());
                log.error(msg, e);
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }
}
