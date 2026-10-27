package com.ruoyi.framework.web.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.UserStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * Spring Security 用户详情服务 —— 认证链入口
 *
 * <p>设计意图：实现 Spring Security 的 {@link UserDetailsService} 接口，
 * 是整个认证链的第一环。Spring Security 的 {@code AuthenticationManager.authenticate()}
 * 会自动回调本类的 {@link #loadUserByUsername} 方法来加载用户信息，
 * 然后依次调用 {@link SysPasswordService#validate} 进行密码校验。</p>
 *
 * <p>认证链调用顺序：</p>
 * <pre>{@code
 * AuthenticationManager.authenticate
 *   → UserDetailsServiceImpl.loadUserByUsername  // 加载用户 + 构建 LoginUser
 *     → SysPasswordService.validate             // 密码匹配 + 重试限制
 *   → 返回 Authentication（Principal = LoginUser）
 * }</pre>
 *
 * <p>loadUserByUsername 执行流程：</p>
 * <ol>
 *   <li>从数据库查询用户（ISysUserService.selectUserByUserName）</li>
 *   <li>用户不存在 → 抛出 ServiceException（用户不存在）</li>
 *   <li>用户已删除（del_flag = 2）→ 抛出 ServiceException</li>
 *   <li>用户已停用（status = 1）→ 抛出 ServiceException</li>
 *   <li>调用 {@link SysPasswordService#validate} 执行密码校验和重试限制</li>
 *   <li>调用 {@link #createLoginUser} 构建完整的 {@link LoginUser}（含权限集合）</li>
 * </ol>
 *
 * <p>在整体架构中的位置：</p>
 * <ul>
 *   <li>登录时由 {@link SysLoginService} 通过 AuthenticationManager 触发</li>
 *   <li>密码校验失败由 {@link SysPasswordService} 在本类调用后执行</li>
 *   <li>权限数据由 {@link SysPermissionService} 聚合</li>
 * </ul>
 *
 * @author ruoyi
 * @see SysLoginService 触发 AuthenticationManager 认证链
 * @see SysPasswordService 在本类中被调用执行密码校验
 * @see SysPermissionService 提供角色和菜单权限数据
 * @see LoginUser Spring Security 的 Principal 实现类
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService
{
    /** 用户加载过程的日志记录器，用于排查登录失败原因 */
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    /** 用户数据库操作服务 */
    @Autowired
    private ISysUserService userService;
    
    /** 密码校验 + 重试次数限制服务 */
    @Autowired
    private SysPasswordService passwordService;

    /** 角色/菜单权限数据聚合服务 */
    @Autowired
    private SysPermissionService permissionService;

    /**
     * Spring Security 认证链回调入口 —— 加载用户并执行密码校验。
     *
     * <p>此方法由 Spring Security 的 DaoAuthenticationProvider 自动调用，
     * 每次调用都会触发一次数据库查询（登录场景下仅调用一次）。</p>
     *
     * <p>校验顺序：用户存在性 → 删除状态 → 停用状态 → 密码匹配 + 重试次数 → 构建 LoginUser。</p>
     *
     * @param username 登录时提交的用户名
     * @return 完整的 {@link LoginUser} 对象（包含用户信息、权限集合、角色列表）
     * @throws UsernameNotFoundException 用户名不存在（Spring Security 标准异常，这里用 ServiceException 替代）
     * @throws ServiceException 用户不存在 / 已删除 / 已停用 / 密码错误 / 重试超限
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        SysUser user = userService.selectUserByUserName(username);
        if (StringUtils.isNull(user))
        {
            log.info("登录用户：{} 不存在.", username);
            throw new ServiceException(MessageUtils.message("user.not.exists"));
        }
        else if (UserStatus.DELETED.getCode().equals(user.getDelFlag()))
        {
            log.info("登录用户：{} 已被删除.", username);
            throw new ServiceException(MessageUtils.message("user.password.delete"));
        }
        else if (UserStatus.DISABLE.getCode().equals(user.getStatus()))
        {
            log.info("登录用户：{} 已被停用.", username);
            throw new ServiceException(MessageUtils.message("user.blocked"));
        }

        passwordService.validate(user);

        return createLoginUser(user);
    }

    /**
     * 根据 SysUser 构建 LoginUser —— 聚合权限集合。
     *
     * <p>调用 {@link SysPermissionService#getMenuPermission} 查询并聚合该用户所有角色对应的菜单权限字符串，
     * 打包到 LoginUser.permissions 中，用于后续接口权限校验。</p>
     *
     * @param user 数据库查询到的用户实体
     * @return 封装好的 LoginUser（包含 userId、deptId、user、permissions）
     */
    public UserDetails createLoginUser(SysUser user)
    {
        return new LoginUser(user.getUserId(), user.getDeptId(), user, permissionService.getMenuPermission(user));
    }
}
