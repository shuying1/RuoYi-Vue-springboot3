package com.ruoyi.web.controller.system;

import java.util.List;
import java.util.Map;
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
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysDeptService;

/**
 * 部门管理接口控制器
 *
 * <p>业务模块：系统管理 — 部门管理，路由前缀 {@code /system/dept}，依赖 Service：{@link ISysDeptService}。
 * 负责组织架构树（SysDept）的增删改查与排序，部门同时承载数据权限（dataScope）的计算维度：
 * 角色绑定部门后，其下用户在查询业务数据时会被 Mapper 层拦截器自动追加 {@code dept_id IN (...)} 过滤条件。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/dept/list}                 — 查询部门列表（树结构）</li>
 *   <li>{@code GET    /system/dept/list/exclude/{deptId}} — 排除指定节点及其所有子节点（用于"上级部门"下拉选择）</li>
 *   <li>{@code GET    /system/dept/{deptId}}             — 获取部门详情</li>
 *   <li>{@code POST   /system/dept}                      — 新增部门</li>
 *   <li>{@code PUT    /system/dept}                      — 修改部门</li>
 *   <li>{@code PUT    /system/dept/updateSort}           — 批量调整部门排序</li>
 *   <li>{@code DELETE /system/dept/{deptId}}             — 删除部门（受保护：有子部门或已分配用户的不可删）</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController extends BaseController
{
    /** 部门 Service */
    @Autowired
    private ISysDeptService deptService;

    /**
     * 获取部门列表（不分页，返回树结构扁平数据）
     *
     * <p>url: GET /system/dept/list</p>
     *
     * @param url 请求路径
     * @param dept 查询条件（deptName、status、parentId 等）
     * @return AjaxResult#success(List<SysDept>)
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public AjaxResult list(SysDept dept)
    {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return success(depts);
    }

    /**
     * 查询部门列表（排除指定节点及其所有后代）
     *
     * <p>url: GET /system/dept/list/exclude/{deptId}</p>
     *
     * <p>用于"上级部门"下拉选择，避免把自己选为自己的上级。</p>
     *
     * @param url 请求路径
     * @param deptId 需要排除的部门主键（路径变量，可为空）
     * @return AjaxResult#success(List<SysDept>)
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId)
    {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        depts.removeIf(d -> d.getDeptId().intValue() == deptId || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));
        return success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     *
     * <p>url: GET /system/dept/{deptId}</p>
     *
     * <p>Service 层会先调用 {@code checkDeptDataScope} 校验当前用户是否有权查看该部门。</p>
     *
     * @param url 请求路径
     * @param deptId 部门主键
     * @return AjaxResult#success(SysDept)
     */
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId)
    {
        deptService.checkDeptDataScope(deptId);
        return success(deptService.selectDeptById(deptId));
    }

    /**
     * 新增部门
     *
     * <p>url: POST /system/dept</p>
     *
     * <p>会校验部门名称唯一性；自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param dept 部门实体（经 {@code @Validated} 校验）
     * @return AjaxResult 成功/失败
     */
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDept dept)
    {
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(getUsername());
        return toAjax(deptService.insertDept(dept));
    }

    /**
     * 修改部门
     *
     * <p>url: PUT /system/dept</p>
     *
     * <p>除常规唯一性校验外，还会拦截三种非法场景：把上级部门设为自己、
     * 停用仍有正常子部门的部门、以及无数据权限范围的部门。</p>
     *
     * @param url 请求路径
     * @param dept 部门实体（含 deptId，经 {@code @Validated} 校验）
     * @return AjaxResult 成功/失败
     */
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDept dept)
    {
        Long deptId = dept.getDeptId();
        deptService.checkDeptDataScope(deptId);
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        else if (dept.getParentId().equals(deptId))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus()) && deptService.selectNormalChildrenDeptById(deptId) > 0)
        {
            return error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(getUsername());
        return toAjax(deptService.updateDept(dept));
    }

    /**
     * 批量调整部门排序
     *
     * <p>url: PUT /system/dept/updateSort</p>
     *
     * <p>请求体（JSON）结构：{@code {"deptIds": "100,101", "orderNums": "1,2"}}。</p>
     *
     * @param url 请求路径
     * @param params 含 deptIds 与 orderNums 的 Map
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @Log(title = "保存部门排序", businessType = BusinessType.UPDATE)
    @PutMapping("/updateSort")
    public AjaxResult updateSort(@RequestBody Map<String, String> params)
    {
        String[] deptIds = params.get("deptIds").split(",");
        String[] orderNums = params.get("orderNums").split(",");
        deptService.updateDeptSort(deptIds, orderNums);
        return success();
    }

    /**
     * 删除部门
     *
     * <p>url: DELETE /system/dept/{deptId}</p>
     *
     * <p>受保护逻辑：存在子部门或已分配用户的部门不允许删除；同时校验数据权限范围。</p>
     *
     * @param url 请求路径
     * @param deptId 需要删除的部门主键
     * @return AjaxResult 成功/警告/失败
     */
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId)
    {
        if (deptService.hasChildByDeptId(deptId))
        {
            return warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId))
        {
            return warn("部门存在用户,不允许删除");
        }
        deptService.checkDeptDataScope(deptId);
        return toAjax(deptService.deleteDeptById(deptId));
    }
}
