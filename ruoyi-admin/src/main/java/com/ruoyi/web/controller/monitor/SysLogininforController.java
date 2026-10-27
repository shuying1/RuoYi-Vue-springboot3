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
import com.ruoyi.framework.web.service.SysPasswordService;
import com.ruoyi.system.domain.SysLogininfor;
import com.ruoyi.system.service.ISysLogininforService;

/**
 * 系统访问记录 Controller
 *
 * <p>业务模块：系统监控 — 登录日志管理，路由前缀 {@code /monitor/logininfor}。
 * 负责查询/导出/删除登录日志（sys_logininfor），并提供"账户解锁"快捷入口（清除密码错误缓存）。</p>
 *
 * <p>依赖 Service：{@link ISysLogininforService}（登录日志 CRUD）、
 * {@link SysPasswordService}（密码错误次数缓存清理）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/logininfor")
public class SysLogininforController extends BaseController
{
    /** 登录日志 Service */
    @Autowired
    private ISysLogininforService logininforService;

    /** 密码策略服务：用于解锁时清除登录失败次数缓存 */
    @Autowired
    private SysPasswordService passwordService;

    /**
     * 分页查询登录日志列表
     *
     * <p>url: GET /monitor/logininfor/list</p>
     *
     * @param url 请求路径
     * @param logininfor 查询条件（userName、ipaddr、status、beginTime/endTime 等）
     * @return TableDataInfo 包含分页元数据与日志记录列表
     */
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysLogininfor logininfor)
    {
        startPage();
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        return getDataTable(list);
    }

    /**
     * 导出登录日志到 Excel
     *
     * <p>url: POST /monitor/logininfor/export</p>
     *
     * <p>权限：{@code monitor:logininfor:export}。自动记录审计日志（业务类型 EXPORT）。</p>
     *
     * @param url 请求路径
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param logininfor 导出筛选条件
     */
    @Log(title = "登录日志", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysLogininfor logininfor)
    {
        List<SysLogininfor> list = logininforService.selectLogininforList(logininfor);
        ExcelUtil<SysLogininfor> util = new ExcelUtil<SysLogininfor>(SysLogininfor.class);
        util.exportExcel(response, list, "登录日志");
    }

    /**
     * 批量删除登录日志
     *
     * <p>url: DELETE /monitor/logininfor/{infoIds}</p>
     *
     * @param url 请求路径
     * @param infoIds 需要删除的登录日志主键数组（路径变量，多个用逗号分隔）
     * @return AjaxResult 成功/失败
     */
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.DELETE)
    @DeleteMapping("/{infoIds}")
    public AjaxResult remove(@PathVariable Long[] infoIds)
    {
        return toAjax(logininforService.deleteLogininforByIds(infoIds));
    }

    /**
     * 清空全部登录日志
     *
     * <p>url: DELETE /monitor/logininfor/clean</p>
     *
     * <p>危险操作：清空 sys_logininfor 表全部数据。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:remove')")
    @Log(title = "登录日志", businessType = BusinessType.CLEAN)
    @DeleteMapping("/clean")
    public AjaxResult clean()
    {
        logininforService.cleanLogininfor();
        return success();
    }

    /**
     * 账户解锁（清除密码错误次数缓存）
     *
     * <p>url: GET /monitor/logininfor/unlock/{userName}</p>
     *
     * <p>当用户因连续输错密码被锁定时，通过本接口解除锁定。底层会删除
     * Redis 中 key 为 {@code pwd_err_cnt:{userName}} 的计数。</p>
     *
     * @param url 请求路径
     * @param userName 需要解锁的登录账号
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('monitor:logininfor:unlock')")
    @Log(title = "账户解锁", businessType = BusinessType.OTHER)
    @GetMapping("/unlock/{userName}")
    public AjaxResult unlock(@PathVariable("userName") String userName)
    {
        passwordService.clearLoginRecordCache(userName);
        return success();
    }
}
