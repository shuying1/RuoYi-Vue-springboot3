package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysPostService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 用户管理接口控制器
 * 
 * <p>负责系统用户（SysUser）的增删改查、角色绑定、岗位分配、密码重置、状态启停等管理功能，
 * 路由前缀为 {@code /system/user}。所有写操作均标注了 {@code @Log} 用于审计日志，
 * 并在 Service 层通过 {@code checkUserDataScope} / {@code checkRoleDataScope} /
 * {@code checkDeptDataScope} 校验当前用户的数据权限范围（基于角色绑定的数据范围）。</p>
 * 
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/user/list}            — 分页查询用户列表</li>
 *   <li>{@code POST   /system/user/export}          — 导出用户 Excel</li>
 *   <li>{@code POST   /system/user/importData}      — 导入用户 Excel</li>
 *   <li>{@code POST   /system/user/importTemplate}   — 下载导入模板</li>
 *   <li>{@code GET    /system/user/{userId}}        — 获取用户详情（含岗位、角色 IDs）</li>
 *   <li>{@code POST   /system/user}                 — 新增用户</li>
 *   <li>{@code PUT    /system/user}                 — 修改用户</li>
 *   <li>{@code DELETE /system/user/{userIds}}       — 删除用户（批量）</li>
 *   <li>{@code PUT    /system/user/resetPwd}        — 重置用户密码</li>
 *   <li>{@code PUT    /system/user/changeStatus}     — 修改用户启停状态</li>
 *   <li>{@code GET    /system/user/authRole/{userId}}  — 查询用户已分配角色</li>
 *   <li>{@code PUT    /system/user/authRole}        — 为用户分配角色</li>
 *   <li>{@code GET    /system/user/deptTree}        — 部门树选择器</li>
 * </ul>
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController extends BaseController
{
    /** 用户服务：用户 CRUD、唯一性校验、导入导出 */
    @Autowired
    private ISysUserService userService;

    /** 角色服务：用于获取角色下拉选项、检查角色数据范围 */
    @Autowired
    private ISysRoleService roleService;

    /** 部门服务：用于获取部门树、检查部门数据范围 */
    @Autowired
    private ISysDeptService deptService;

    /** 岗位服务：用于获取岗位下拉选项、按用户查询岗位 */
    @Autowired
    private ISysPostService postService;

    /**
     * 分页查询用户列表
     * 
     * <p>GET /system/user/list</p>
     * 
     * <p>通过 MyBatis PageHelper 自动分页，查询条件（deptId、userName、phonenumber、status 等）
     * 封装在 {@link SysUser} 实体中。Service 层会自动应用当前用户的数据范围过滤。</p>
     * 
     * @param user 查询条件对象，支持 deptId、userName、phonenumber、status、beginTime/endTime 等
     * @return TableDataInfo 包含 total 总条数与 rows 数据列表
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    /**
     * 导出用户列表到 Excel
     * 
     * <p>POST /system/user/export</p>
     * 
     * <p>响应类型 {@code application/vnd.openxmlformats-officedocument.spreadsheetml.sheet}，
     * 文件名形如 {@code 用户数据_20240101.xlsx}。</p>
     * 
     * @param response 用于写入 Excel 二进制流
     * @param user 导出条件，与 list 接口相同
     */
    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:user:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    /**
     * 批量导入用户
     * 
     * <p>POST /system/user/importData（multipart/form-data）</p>
     * 
     * @param file Excel 文件
     * @param updateSupport 是否对已存在的用户执行更新，false 则遇到重复数据直接失败
     * @return AjaxResult 成功时 data 包含导入结果统计字符串
     * @throws Exception Excel 解析异常
     */
    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @PreAuthorize("@ss.hasPermi('system:user:import')")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    /**
     * 下载用户导入 Excel 模板
     * 
     * <p>POST /system/user/importTemplate</p>
     */
    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response)
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 根据用户编号获取用户详情
     * 
     * <p>GET /system/user/{userId} 或 GET /system/user/（新增模式下只返回下拉数据）</p>
     * 
     * <p>响应 data 字段结构：</p>
     * <pre>
     * {
     *   "data":       SysUser,             // 用户实体（更新模式下）
     *   "postIds":    [1, 2],              // 用户已分配的岗位 ID
     *   "roleIds":    [100, 101],          // 用户已分配的角色 ID
     *   "roles":      [SysRole, ...],      // 全部角色下拉选项
     *   "posts":      [SysPost, ...]       // 全部岗位下拉选项
     * }
     * </pre>
     * 
     * @param userId 用户主键，可为 null（新增场景）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    /**
     * 新增用户
     * 
     * <p>POST /system/user</p>
     * 
     * <p>请求体包含 userName、nickName、password、deptId、roleIds、postIds、phonenumber、email 等字段。
     * 接口会校验用户名、手机号、邮箱唯一性，以及部门/角色数据权限范围，
     * 密码会通过 {@link SecurityUtils#encryptPassword} 使用 BCrypt 加密后入库。</p>
     * 
     * @param user 用户实体
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户信息
     * 
     * <p>PUT /system/user</p>
     * 
     * <p>请求体需携带 userId。会校验当前用户是否允许操作该用户（不能修改 admin，不能修改自己无权限范围的用户），
     * 同时校验用户名、手机号、邮箱唯一性。</p>
     * 
     * @param user 用户实体（含 userId）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 批量删除用户
     * 
     * <p>DELETE /system/user/{userIds}，路径变量支持多个 userId（逗号分隔）</p>
     * 
     * <p>受保护逻辑：当前登录用户自身不能被删除；admin 用户不能被普通管理员删除（Service 层拦截）。</p>
     * 
     * @param userIds 用户主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        if (ArrayUtils.contains(userIds, getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置用户密码
     * 
     * <p>PUT /system/user/resetPwd</p>
     * 
     * <p>请求体需携带 userId 与新密码 newPassword，接口会做 BCrypt 加密后入库。
     * 注意：不能对 admin 用户、也不能对无数据权限范围的用户重置密码。</p>
     * 
     * @param user 请求体（含 userId + newPassword）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:resetPwd')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 修改用户启停状态
     * 
     * <p>PUT /system/user/changeStatus</p>
     * 
     * <p>请求体携带 userId 与 status（0=正常 1=停用）。停用后该用户的现有 token 仍可使用直到过期，
     * 但 {@link TokenService#verifyToken} 校验时会通过 {@code checkAllowed} 拦截被停用用户。</p>
     * 
     * @param user 请求体（含 userId + status）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 查询用户已分配角色
     * 
     * <p>GET /system/user/authRole/{userId}</p>
     * 
     * <p>响应 data 结构：</p>
     * <pre>
     * {
     *   "user":  SysUser,              // 用户基本信息
     *   "roles": [SysRole, ...]       // 该用户当前已分配的角色列表（admin 用户会过滤掉 admin 角色）
     * }
     * </pre>
     * 
     * @param userId 用户主键
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 为用户分配角色（全量覆盖）
     * 
     * <p>PUT /system/user/authRole</p>
     * 
     * <p>请求参数（query string）：</p>
     * <ul>
     *   <li>{@code userId} — 用户主键</li>
     *   <li>{@code roleIds} — 角色 ID 数组（逗号分隔）</li>
     * </ul>
     * 
     * <p>Service 层会先删除原关联再批量插入新关联（sys_user_role 表）。</p>
     * 
     * @param userId 用户主键
     * @param roleIds 要分配的角色 ID 数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表（用于用户管理中的部门选择器）
     * 
     * <p>GET /system/user/deptTree</p>
     * 
     * <p>Service 层会按当前用户的数据权限过滤可见部门节点，并组装成前端 el-tree 所需的树形结构。</p>
     * 
     * @param dept 查询条件，可为空
     * @return AjaxResult 包含 List<TreeSelect>，每个节点含 id、label、children
     */
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }
}
