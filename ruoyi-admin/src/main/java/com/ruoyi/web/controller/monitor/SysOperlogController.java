package com.ruoyi.web.controller.monitor;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysOperLog;
import com.ruoyi.system.service.ISysOperLogService;

/**
 * 操作日志记录 Controller
 *
 * <p>业务模块：系统监控 — 操作日志管理，路由前缀 {@code /monitor/operlog}。
 * 负责查询/导出/清空 sys_operlog 表。操作日志由 AOP 切面 {@code @Log} 注解在业务方法执行时自动写入。</p>
 *
 * <p>依赖 Service：{@link ISysOperLogService}。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/operlog")
public class SysOperlogController extends BaseController
{
    /** 操作日志 Service */
    @Autowired
    private ISysOperLogService operLogService;

    /**
     * 分页查询操作日志列表
     *
     * <p>url: GET /monitor/operlog/list</p>
     *
     * @param url 请求路径
     * @param operLog 查询条件（title、operName、businessType、status、beginTime/endTime 等）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('monitor:operlog:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysOperLog operLog)
    {
        startPage();
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        return getDataTable(list);
    }

    /**
     * 导出操作日志到 Excel
     *
     * <p>url: POST /monitor/operlog/export</p>
     *
     * @param url 请求路径
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param operLog 导出筛选条件
     */
    @Log(title = "操作日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysOperLog operLog)
    {
        List<SysOperLog> list = operLogService.selectOperLogList(operLog);
        ExcelUtil<SysOperLog> util = new ExcelUtil<SysOperLog>(SysOperLog.class);
        util.exportExcel(response, list, "操作日志");
    }

    /**
     * 批量删除操作日志
     *
     * <p>url: DELETE /monitor/operlog/{operIds}</p>
     *
     * @param url 请求路径
     * @param operIds 需要删除的操作日志主键数组
     * @return AjaxResult 成功/失败
     */
    @Log(title = "操作日志", businessType = BusinessType.DELETE)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/{operIds}")
    public AjaxResult remove(@PathVariable Long[] operIds)
    {
        return toAjax(operLogService.deleteOperLogByIds(operIds));
    }

    /**
     * 清空全部操作日志
     *
     * <p>url: DELETE /monitor/operlog/clean</p>
     *
     * <p>危险操作：清空 sys_operlog 表全部数据。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success()
     */
    @Log(title = "操作日志", businessType = BusinessType.CLEAN)
    @PreAuthorize("@ss.hasPermi('monitor:operlog:remove')")
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        operLogService.cleanOperLog();
        return success();
    }
}
