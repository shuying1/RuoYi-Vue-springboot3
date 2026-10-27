package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysMenuService;

/**
 * 菜单管理接口控制器
 *
 * <p>业务模块：系统管理 — 菜单管理，路由前缀 {@code /system/menu}，依赖 Service：{@link ISysMenuService}。
 * 负责系统菜单、目录、按钮的三层树形结构管理。菜单是 RBAC 模型中最细粒度的权限载体：
 * 用户通过角色关联菜单，{@code perms} 字段（如 {@code system:user:list}）供 {@code @PreAuthorize} 注解做方法级鉴权。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/menu/list}                    — 查询菜单列表（按当前用户权限过滤）</li>
 *   <li>{@code GET    /system/menu/{menuId}}                — 获取菜单详情</li>
 *   <li>{@code GET    /system/menu/treeselect}              — 菜单下拉树选择器</li>
 *   <li>{@code GET    /system/menu/roleMenuTreeselect/{roleId}} — 角色菜单分配弹窗数据</li>
 *   <li>{@code POST   /system/menu}                         — 新增菜单</li>
 *   <li>{@code PUT    /system/menu}                         — 修改菜单</li>
 *   <li>{@code PUT    /system/menu/updateSort}               — 批量调整菜单显示顺序</li>
 *   <li>{@code DELETE /system/menu/{menuId}}                 — 删除菜单（受保护：有子菜单或已分配角色的不可删）</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuController extends BaseController
{
    /** 菜单 Service */
    @Autowired
    private ISysMenuService menuService;

    /**
     * 获取菜单列表
     *
     * <p>url: GET /system/menu/list</p>
     *
     * <p>Service 层会根据当前用户的角色权限自动过滤，只返回该用户可见的菜单节点。
     * 支持 menuName、status、visible 等查询条件。</p>
     *
     * @param url 请求路径
     * @param menu 查询条件对象
     * @return AjaxResult#success(List<SysMenu>)
     */
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(SysMenu menu)
    {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menus);
    }

    /**
     * 根据菜单编号获取详细信息
     *
     * <p>url: GET /system/menu/{menuId}</p>
     *
     * @param url 请求路径
     * @param menuId 菜单主键
     * @return AjaxResult#success(SysMenu)
     */
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping(value = "/{menuId}")
    public AjaxResult getInfo(@PathVariable Long menuId)
    {
        return success(menuService.selectMenuById(menuId));
    }

    /**
     * 获取菜单下拉树列表
     *
     * <p>url: GET /system/menu/treeselect</p>
     *
     * <p>用于新增/编辑菜单时的"上级菜单"选择器；按当前用户权限过滤。</p>
     *
     * @param url 请求路径
     * @param menu 查询条件
     * @return AjaxResult#success(List<TreeSelect>)
     */
    @GetMapping("/treeselect")
    public AjaxResult treeselect(SysMenu menu)
    {
        List<SysMenu> menus = menuService.selectMenuList(menu, getUserId());
        return success(menuService.buildMenuTreeSelect(menus));
    }

    /**
     * 加载对应角色菜单列表树（角色权限分配弹窗数据）
     *
     * <p>url: GET /system/menu/roleMenuTreeselect/{roleId}</p>
     *
     * <p>响应结构：</p>
     * <pre>
     * {
     *   "checkedKeys": [100, 101, 102],   // 该角色已勾选的菜单 ID
     *   "menus":       [TreeSelect, ...]  // 完整菜单树（按当前用户权限过滤）
     * }
     * </pre>
     *
     * @param url 请求路径
     * @param roleId 角色主键
     * @return AjaxResult 包含 checkedKeys 与 menus
     */
    @GetMapping(value = "/roleMenuTreeselect/{roleId}")
    public AjaxResult roleMenuTreeselect(@PathVariable("roleId") Long roleId)
    {
        List<SysMenu> menus = menuService.selectMenuList(getUserId());
        AjaxResult ajax = AjaxResult.success();
        ajax.put("checkedKeys", menuService.selectMenuListByRoleId(roleId));
        ajax.put("menus", menuService.buildMenuTreeSelect(menus));
        return ajax;
    }

    /**
     * 新增菜单
     *
     * <p>url: POST /system/menu</p>
     *
     * <p>会校验菜单名称、路由名称/路由地址的唯一性；
     * 若设置为外链（isFrame=1）则 path 必须以 http(s):// 开头；
     * 自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param menu 菜单实体（经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @Log(title = "菜单管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysMenu menu)
    {
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (!menuService.checkRouteConfigUnique(menu))
        {
            return error("新增菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        menu.setCreateBy(getUsername());
        return toAjax(menuService.insertMenu(menu));
    }

    /**
     * 修改菜单
     *
     * <p>url: PUT /system/menu</p>
     *
     * <p>校验逻辑同 {@link #add}，另外禁止把上级菜单设为自己；自动写入 updateBy。</p>
     *
     * @param url 请求路径
     * @param menu 菜单实体（含 menuId，经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "菜单管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysMenu menu)
    {
        if (!menuService.checkMenuNameUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，菜单名称已存在");
        }
        else if (UserConstants.YES_FRAME.equals(menu.getIsFrame()) && !StringUtils.ishttp(menu.getPath()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，地址必须以http(s)://开头");
        }
        else if (menu.getMenuId().equals(menu.getParentId()))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，上级菜单不能选择自己");
        }
        else if (!menuService.checkRouteConfigUnique(menu))
        {
            return error("修改菜单'" + menu.getMenuName() + "'失败，路由名称或地址已存在");
        }
        menu.setUpdateBy(getUsername());
        return toAjax(menuService.updateMenu(menu));
    }

    /**
     * 批量调整菜单显示顺序
     *
     * <p>url: PUT /system/menu/updateSort</p>
     *
     * <p>请求体（JSON）结构：{@code {"menuIds": "1,2,3", "orderNums": "1,2,3"}}。</p>
     *
     * @param url 请求路径
     * @param params 含 menuIds 与 orderNums 的 Map
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @Log(title = "保存菜单排序", businessType = BusinessType.UPDATE)
    @PutMapping("/updateSort")
    public AjaxResult updateSort(@RequestBody Map<String, String> params)
    {
        String[] menuIds = params.get("menuIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        menuService.updateMenuSort(menuIds, orderNums);
        return success();
    }

    /**
     * 删除菜单
     *
     * <p>url: DELETE /system/menu/{menuId}</p>
     *
     * <p>受保护逻辑：存在子菜单或已分配角色的菜单不允许删除。</p>
     *
     * @param url 请求路径
     * @param menuId 需要删除的菜单主键
     * @return AjaxResult 成功/警告
     */
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @Log(title = "菜单管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{menuId}")
    public AjaxResult remove(@PathVariable("menuId") Long menuId)
    {
        if (menuService.hasChildByMenuId(menuId))
        {
            return warn("存在子菜单,不允许删除");
        }
        if (menuService.checkMenuExistRole(menuId))
        {
            return warn("菜单已分配,不允许删除");
        }
        return toAjax(menuService.deleteMenuById(menuId));
    }
}
