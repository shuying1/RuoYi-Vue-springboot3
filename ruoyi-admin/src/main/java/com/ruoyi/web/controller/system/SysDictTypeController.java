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
import com.ruoyi.common.core.domain.entity.SysDictType;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.service.ISysDictTypeService;

/**
 * 字典类型管理接口控制器
 *
 * <p>业务模块：系统管理 — 字典管理，路由前缀 {@code /system/dict/type}，依赖 Service：{@link ISysDictTypeService}。
 * 负责数据字典类型（SysDictType）的增删改查与缓存刷新。字典是前端下拉/单选/复选的数据来源，
 * 字典类型本身只描述"分类"，具体选项由 {@link SysDictDataController} 管理（挂在 {@code /system/dict/data} 下）。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/dict/type/list}           — 分页查询字典类型列表</li>
 *   <li>{@code POST   /system/dict/type/export}         — 导出字典类型 Excel</li>
 *   <li>{@code GET    /system/dict/type/{dictId}}       — 获取字典类型详情</li>
 *   <li>{@code POST   /system/dict/type}                — 新增字典类型</li>
 *   <li>{@code PUT    /system/dict/type}                — 修改字典类型</li>
 *   <li>{@code DELETE /system/dict/type/{dictIds}}      — 批量删除字典类型（会级联删除字典数据）</li>
 *   <li>{@code DELETE /system/dict/type/refreshCache}  — 刷新 Redis 中的字典缓存</li>
 *   <li>{@code GET    /system/dict/type/optionselect}   — 字典类型下拉选择器</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/dict/type")
public class SysDictTypeController extends BaseController
{
    /** 字典类型 Service */
    @Autowired
    private ISysDictTypeService dictTypeService;

    /**
     * 分页查询字典类型列表
     *
     * <p>url: GET /system/dict/type/list</p>
     *
     * @param url 请求路径
     * @param dictType 查询条件
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDictType dictType)
    {
        startPage();
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        return getDataTable(list);
    }

    /**
     * 导出字典类型列表到 Excel
     *
     * <p>url: POST /system/dict/type/export</p>
     *
     * @param url 请求路径
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param dictType 导出筛选条件
     */
    @Log(title = "字典类型", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:dict:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDictType dictType)
    {
        List<SysDictType> list = dictTypeService.selectDictTypeList(dictType);
        ExcelUtil<SysDictType> util = new ExcelUtil<SysDictType>(SysDictType.class);
        util.exportExcel(response, list, "字典类型");
    }

    /**
     * 查询字典类型详细
     *
     * <p>url: GET /system/dict/type/{dictId}</p>
     *
     * @param url 请求路径
     * @param dictId 字典类型主键
     * @return AjaxResult#success(SysDictType)
     */
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping(value = "/{dictId}")
    public AjaxResult getInfo(@PathVariable Long dictId)
    {
        return success(dictTypeService.selectDictTypeById(dictId));
    }

    /**
     * 新增字典类型
     *
     * <p>url: POST /system/dict/type</p>
     *
     * <p>会校验 dictType 类型标识唯一性；自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param dict 字典类型实体（经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @Log(title = "字典类型", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDictType dict)
    {
        if (!dictTypeService.checkDictTypeUnique(dict))
        {
            return error("新增字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        dict.setCreateBy(getUsername());
        return toAjax(dictTypeService.insertDictType(dict));
    }

    /**
     * 修改字典类型
     *
     * <p>url: PUT /system/dict/type</p>
     *
     * <p>会校验 dictType 类型标识唯一性；自动将当前操作用户写入 updateBy。</p>
     *
     * @param url 请求路径
     * @param dict 字典类型实体（含 dictId，经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @Log(title = "字典类型", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDictType dict)
    {
        if (!dictTypeService.checkDictTypeUnique(dict))
        {
            return error("修改字典'" + dict.getDictName() + "'失败，字典类型已存在");
        }
        dict.setUpdateBy(getUsername());
        return toAjax(dictTypeService.updateDictType(dict));
    }

    /**
     * 批量删除字典类型
     *
     * <p>url: DELETE /system/dict/type/{dictIds}</p>
     *
     * <p>删除字典类型会级联删除其下所有字典数据（由 Service 层处理）。</p>
     *
     * @param url 请求路径
     * @param dictIds 需要删除的字典类型主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictIds}")
    public AjaxResult remove(@PathVariable Long[] dictIds)
    {
        dictTypeService.deleteDictTypeByIds(dictIds);
        return success();
    }

    /**
     * 刷新 Redis 中的字典缓存
     *
     * <p>url: DELETE /system/dict/type/refreshCache</p>
     *
     * <p>重新从数据库全量加载字典类型与字典数据到 Redis。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @Log(title = "字典类型", businessType = BusinessType.CLEAN)
    @DeleteMapping("/refreshCache")
    public AjaxResult refreshCache()
    {
        dictTypeService.resetDictCache();
        return success();
    }

    /**
     * 获取字典类型下拉选择框列表
     *
     * <p>url: GET /system/dict/type/optionselect</p>
     *
     * <p>用于字典数据新增/编辑时的"字典类型"下拉选择。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success(List<SysDictType>)
     */
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        List<SysDictType> dictTypes = dictTypeService.selectDictTypeAll();
        return success(dictTypes);
    }
}
