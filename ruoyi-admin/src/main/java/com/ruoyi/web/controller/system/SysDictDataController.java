package com.ruoyi.web.controller.system;

import java.util.ArrayList;
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
import com.ruoyi.common.core.domain.entity.SysDictData;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.service.ISysDictDataService;
import com.ruoyi.system.service.ISysDictTypeService;

/**
 * 字典数据管理接口控制器
 *
 * <p>业务模块：系统管理 — 字典管理，路由前缀 {@code /system/dict/data}，
 * 依赖 Service：{@link ISysDictDataService}、{@link ISysDictTypeService}（按类型查询字典时使用）。
 * 负责字典类型下的具体选项（SysDictData）的增删改查，
 * 前端业务页面通常通过 {@code GET /system/dict/data/type/{dictType}} 获取选项列表（公开接口，无需 token）。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/dict/data/list}                 — 分页查询字典数据列表</li>
 *   <li>{@code POST   /system/dict/data/export}               — 导出字典数据 Excel</li>
 *   <li>{@code GET    /system/dict/data/{dictCode}}          — 获取字典数据详情</li>
 *   <li>{@code GET    /system/dict/data/type/{dictType}}     — 按字典类型查询选项（公开接口）</li>
 *   <li>{@code POST   /system/dict/data}                     — 新增字典数据</li>
 *   <li>{@code PUT    /system/dict/data}                     — 修改字典数据</li>
 *   <li>{@code DELETE /system/dict/data/{dictCodes}}         — 批量删除字典数据</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/dict/data")
public class SysDictDataController extends BaseController
{
    /** 字典数据 Service */
    @Autowired
    private ISysDictDataService dictDataService;

    /** 字典类型 Service（用于按类型查询字典数据的公开接口） */
    @Autowired
    private ISysDictTypeService dictTypeService;

    /**
     * 分页查询字典数据列表
     *
     * <p>url: GET /system/dict/data/list</p>
     *
     * @param url 请求路径
     * @param dictData 查询条件（dictType、dictLabel、status 等）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysDictData dictData)
    {
        startPage();
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        return getDataTable(list);
    }

    /**
     * 导出字典数据列表到 Excel
     *
     * <p>url: POST /system/dict/data/export</p>
     *
     * @param url 请求路径
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param dictData 导出筛选条件
     */
    @Log(title = "字典数据", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:dict:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysDictData dictData)
    {
        List<SysDictData> list = dictDataService.selectDictDataList(dictData);
        ExcelUtil<SysDictData> util = new ExcelUtil<SysDictData>(SysDictData.class);
        util.exportExcel(response, list, "字典数据");
    }

    /**
     * 查询字典数据详细
     *
     * <p>url: GET /system/dict/data/{dictCode}</p>
     *
     * @param url 请求路径
     * @param dictCode 字典数据主键
     * @return AjaxResult#success(SysDictData)
     */
    @PreAuthorize("@ss.hasPermi('system:dict:query')")
    @GetMapping(value = "/{dictCode}")
    public AjaxResult getInfo(@PathVariable Long dictCode)
    {
        return success(dictDataService.selectDictDataById(dictCode));
    }

    /**
     * 根据字典类型查询字典数据信息（公开接口）
     *
     * <p>url: GET /system/dict/data/type/{dictType}</p>
     *
     * <p>未加 {@code @PreAuthorize}，匿名可访问；业务前端下拉/单选组件通常用此接口。</p>
     *
     * @param url 请求路径
     * @param dictType 字典类型，如 {@code sys_user_sex}
     * @return AjaxResult#success(List<SysDictData>) 查不到时返回空集合
     */
    @GetMapping(value = "/type/{dictType}")
    public AjaxResult dictType(@PathVariable String dictType)
    {
        List<SysDictData> data = dictTypeService.selectDictDataByType(dictType);
        if (StringUtils.isNull(data))
        {
            data = new ArrayList<SysDictData>();
        }
        return success(data);
    }

    /**
     * 新增字典数据
     *
     * <p>url: POST /system/dict/data</p>
     *
     * <p>自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param dict 字典数据实体（经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @Log(title = "字典数据", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDictData dict)
    {
        dict.setCreateBy(getUsername());
        return toAjax(dictDataService.insertDictData(dict));
    }

    /**
     * 修改保存字典数据
     *
     * <p>url: PUT /system/dict/data</p>
     *
     * <p>自动将当前操作用户写入 updateBy。</p>
     *
     * @param url 请求路径
     * @param dict 字典数据实体（含 dictCode，经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @Log(title = "字典数据", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDictData dict)
    {
        dict.setUpdateBy(getUsername());
        return toAjax(dictDataService.updateDictData(dict));
    }

    /**
     * 批量删除字典数据
     *
     * <p>url: DELETE /system/dict/data/{dictCodes}</p>
     *
     * @param url 请求路径
     * @param dictCodes 需要删除的字典数据主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @Log(title = "字典类型", businessType = BusinessType.DELETE)
    @DeleteMapping("/{dictCodes}")
    public AjaxResult remove(@PathVariable Long[] dictCodes)
    {
        dictDataService.deleteDictDataByIds(dictCodes);
        return success();
    }
}
