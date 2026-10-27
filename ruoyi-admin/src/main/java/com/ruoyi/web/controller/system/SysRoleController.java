package com.ruoyi.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.framework.web.service.SysPermissionService;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.domain.SysUserRole;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysRoleService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 角色管理接口控制器
 * 
 * <p>负责系统角色（SysRole）的增删改查、数据权限分配、菜单权限分配、
 * 以及角色与用户之间的授权关联管理，路由前缀为 {@code /system/role}。</p>
 * 
 * <p>角色是 RBAC 模型的核心桥梁：用户 → 角色 → 菜单/按钮，角色同时绑定数据范围（全部/本部门/本部门及以下/自定义）
 * 用于在 Service/Mapper 层通过注解或拦截器自动拼接 {@code dataScope} 条件过滤 SQL。</p>
 * 
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/role/list}                    — 分页查询角色列表</li>
 *   <li>{@code POST   /system/role/export}                  — 导出角色 Excel</li>
 *   <li>{@code GET    /system/role/{roleId}}                — 获取角色详情</li>
 *   <li>{@code POST   /system/role}                         — 新增角色</li>
 *   <li>{@code PUT    /system/role}                         — 修改角色（会刷新所有在线用户权限缓存）</li>
 *   <li>{@code PUT    /system/role/dataScope}               — 修改角色数据权限范围</li>
 *   <li>{@code PUT    /system/role/changeStatus}            — 修改角色启停状态</li>
 *   <li>{@code DELETE /system/role/{roleIds}}               — 删除角色（批量）</li>
 *   <li>{@code GET    /system/role/optionselect}            — 角色选择器（下拉）</li>
 *   <li>{@code GET    /system/role/authUser/allocatedList}   — 已分配该角色的用户列表</li>
 *   <li>{@code GET    /system/role/authUser/unallocatedList} — 未分配该角色的用户列表</li>
 *   <li>{@code PUT    /system/role/authUser/cancel}         — 取消单个用户的角色</li>
 *   <li>{@code PUT    /system/role/authUser/cancelAll}      — 批量取消用户的角色</li>
 *   <li>{@code PUT    /system/role/authUser/selectAll}      — 批量为用户分配角色</li>
 *   <li>{@code GET    /system/role/deptTree/{roleId}}       — 获取角色已勾选部门 + 完整部门树</li>
 * </ul>
 * 
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/role")
public class SysRoleController extends BaseController
{
    /** 角色服务：角色 CRUD、唯一性校验、数据权限范围配置 */
    @Autowired
    private ISysRoleService roleService;

    /** token 服务：角色变更后刷新在线用户的权限缓存 */
    @Autowired
    private TokenService tokenService;

    /** 权限服务：提供角色/菜单权限查询能力 */
    @Autowired
    private SysPermissionService permissionService;

    /** 用户服务：查询角色下已分配/未分配的用户 */
    @Autowired
    private ISysUserService userService;

    /** 部门服务：提供部门树、按角色查询关联部门 */
    @Autowired
    private ISysDeptService deptService;

    /**
     * 分页查询角色列表
     * 
     * <p>GET /system/role/list</p>
     * 
     * @param role 查询条件对象（roleName、roleKey、status、beginTime/endTime 等）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysRole role)
    {
        startPage();
        List<SysRole> list = roleService.selectRoleList(role);
        return getDataTable(list);
    }

    /**
     * 导出角色列表到 Excel
     * 
     * <p>POST /system/role/export</p>
     * 
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param role 导出条件
     */
    @Log(title = "角色管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:role:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysRole role)
    {
        List<SysRole> list = roleService.selectRoleList(role);
        ExcelUtil<SysRole> util = new ExcelUtil<SysRole>(SysRole.class);
        util.exportExcel(response, list, "角色数据");
    }

    /**
     * 根据角色编号获取角色详情
     * 
     * <p>GET /system/role/{roleId}</p>
     * 
     * @param roleId 角色主键
     * @return AjaxResult#success(SysRole)
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping(value = "/{roleId}")
    public AjaxResult getInfo(@PathVariable Long roleId)
    {
        roleService.checkRoleDataScope(roleId);
        return success(roleService.selectRoleById(roleId));
    }

    /**
     * 新增角色
     * 
     * <p>POST /system/role</p>
     * 
     * <p>会校验角色名称与角色权限标识（roleKey）的唯一性，
     * 并自动将当前操作用户写入 {@code createBy}。</p>
     * 
     * @param role 角色实体
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @Log(title = "角色管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysRole role)
    {
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("新增角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setCreateBy(getUsername());
        return toAjax(roleService.insertRole(role));

    }

    /**
     * 修改保存角色（含菜单权限）
     * 
     * <p>PUT /system/role</p>
     * 
     * <p>角色信息更新成功后，会立即通过 {@link TokenService#refreshPermissionByRoleId}
     * 刷新所有持有该角色的在线用户的 token 权限缓存，保证权限变更即时生效。</p>
     * 
     * @param role 角色实体（含 roleId、menuIds 等）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        if (!roleService.checkRoleNameUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色名称已存在");
        }
        else if (!roleService.checkRoleKeyUnique(role))
        {
            return error("修改角色'" + role.getRoleName() + "'失败，角色权限已存在");
        }
        role.setUpdateBy(getUsername());
        
        if (roleService.updateRole(role) > 0)
        {
            // 刷新所有持有该角色的在线用户权限
            tokenService.refreshPermissionByRoleId(role.getRoleId(), permissionService);
            return success();
        }
        return error("修改角色'" + role.getRoleName() + "'失败，请联系管理员");
    }

    /**
     * 修改保存数据权限范围
     * 
     * <p>PUT /system/role/dataScope</p>
     * 
     * <p>请求体需携带 roleId、dataScope（1=全部 2=自定义 3=本部门 4=本部门及以下 5=仅本人）、
     * 以及可选的 deptIds（当 dataScope=2 时需要勾选的部门 ID 数组）。</p>
     * 
     * @param role 角色实体（含 roleId、dataScope、deptIds）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/dataScope")
    public AjaxResult dataScope(@RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        return toAjax(roleService.authDataScope(role));
    }

    /**
     * 修改角色启停状态
     * 
     * <p>PUT /system/role/changeStatus</p>
     * 
     * @param role 请求体（含 roleId + status）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysRole role)
    {
        roleService.checkRoleAllowed(role);
        roleService.checkRoleDataScope(role.getRoleId());
        role.setUpdateBy(getUsername());
        return toAjax(roleService.updateRoleStatus(role));
    }

    /**
     * 批量删除角色
     * 
     * <p>DELETE /system/role/{roleIds}</p>
     * 
     * <p>Service 层会先校验角色是否已分配给用户，若存在关联则拒绝删除。
     * admin 角色不允许被删除。</p>
     * 
     * @param roleIds 角色主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @Log(title = "角色管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{roleIds}")
    public AjaxResult remove(@PathVariable Long[] roleIds)
    {
        return toAjax(roleService.deleteRoleByIds(roleIds));
    }

    /**
     * 获取角色下拉选择框列表
     * 
     * <p>GET /system/role/optionselect</p>
     * 
     * <p>返回所有角色（含 admin），常用于新增用户/角色选择器场景。</p>
     * 
     * @return AjaxResult#success(List<SysRole>)
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        return success(roleService.selectRoleAll());
    }

    /**
     * 查询已分配该角色的用户列表
     * 
     * <p>GET /system/role/authUser/allocatedList，通过 query string 传入 roleId</p>
     * 
     * @param user 查询条件（含 roleId）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/authUser/allocatedList")
    public TableDataInfo allocatedList(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectAllocatedList(user);
        return getDataTable(list);
    }

    /**
     * 查询未分配该角色的用户列表
     * 
     * <p>GET /system/role/authUser/unallocatedList，通过 query string 传入 roleId</p>
     * 
     * @param user 查询条件（含 roleId）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/authUser/unallocatedList")
    public TableDataInfo unallocatedList(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUnallocatedList(user);
        return getDataTable(list);
    }

    /**
     * 取消单个用户的角色
     * 
     * <p>PUT /system/role/authUser/cancel</p>
     * 
     * @param userRole 请求体（含 userId + roleId）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancel")
    public AjaxResult cancelAuthUser(@RequestBody SysUserRole userRole)
    {
        return toAjax(roleService.deleteAuthUser(userRole));
    }

    /**
     * 批量取消用户的角色
     * 
     * <p>PUT /system/role/authUser/cancelAll</p>
     * 
     * @param roleId 角色主键
     * @param userIds 要移除的用户主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/cancelAll")
    public AjaxResult cancelAuthUserAll(Long roleId, Long[] userIds)
    {
        return toAjax(roleService.deleteAuthUsers(roleId, userIds));
    }

    /**
     * 批量为用户分配角色
     * 
     * <p>PUT /system/role/authUser/selectAll</p>
     * 
     * @param roleId 角色主键
     * @param userIds 要新增分配的用户主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @Log(title = "角色管理", businessType = BusinessType.GRANT)
    @PutMapping("/authUser/selectAll")
    public AjaxResult selectAuthUserAll(Long roleId, Long[] userIds)
    {
        roleService.checkRoleDataScope(roleId);
        return toAjax(roleService.insertAuthUsers(roleId, userIds));
    }

    /**
     * 获取角色部门树（数据权限分配页用）
     * 
     * <p>GET /system/role/deptTree/{roleId}</p>
     * 
     * <p>响应结构：</p>
     * <pre>
     * {
     *   "checkedKeys": [100, 101, 102],   // 该角色 dataScope=2（自定义）时已勾选的部门 ID
     *   "depts":       [SysDeptTree, ...] // 完整部门树
     * }
     * </pre>
     * 
     * @param roleId 角色主键
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping(value = "/deptTree/{roleId}")
    public AjaxResult deptTree(@PathVariable("roleId") Long roleId)
    {
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", deptService.selectDeptListByRoleId(roleId));
        ajax.put("depts", deptService.selectDeptTreeList(new SysDept()));
        return ajax;
    }
}
