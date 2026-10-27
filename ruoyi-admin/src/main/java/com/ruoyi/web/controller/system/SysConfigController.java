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
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 参数配置接口控制器
 *
 * <p>业务模块：系统管理 — 参数配置，路由前缀 {@code /system/config}，依赖 Service：{@link ISysConfigService}。
 * 参数以键值对形式存储在 sys_config 表，启动时全量加载到 Redis，运行期通过
 * {@code ISysConfigService.selectConfigByKey} 读取。常见参数包括验证码开关、
 * 密码复杂度、密码有效期、用户初始密码等。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/config/list}              — 分页查询参数列表</li>
 *   <li>{@code POST   /system/config/export}            — 导出参数 Excel</li>
 *   <li>{@code GET    /system/config/{configId}}        — 获取参数详情</li>
 *   <li>{@code GET    /system/config/configKey/{configKey}} — 按键名查询参数值（公开接口）</li>
 *   <li>{@code POST   /system/config}                   — 新增参数</li>
 *   <li>{@code PUT    /system/config}                   — 修改参数</li>
 *   <li>{@code DELETE /system/config/{configIds}}       — 批量删除参数</li>
 *   <li>{@code DELETE /system/config/refreshCache}     — 刷新 Redis 中的参数缓存</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/config")
public class SysConfigController extends BaseController
{
    /** 参数配置 Service */
    @Autowired
    private ISysConfigService configService;

    /**
     * 分页查询参数配置列表
     *
     * <p>url: GET /system/config/list</p>
     *
     * @param url 请求路径
     * @param config 查询条件（configName、configKey、configType 等）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:config:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysConfig config)
    {
        startPage();
        List<SysConfig> list = configService.selectConfigList(config);
        return getDataTable(list);
    }

    /**
     * 导出参数配置列表到 Excel
     *
     * <p>url: POST /system/config/export</p>
     *
     * <p>自动记录审计日志（业务类型 EXPORT）。</p>
     *
     * @param url 请求路径
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param config 导出筛选条件
     */
    @Log(title = "参数管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:config:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysConfig config)
    {
        List<SysConfig> list = configService.selectConfigList(config);
        ExcelUtil<SysConfig> util = new ExcelUtil<SysConfig>(SysConfig.class);
        util.exportExcel(response, list, "参数数据");
    }

    /**
     * 根据参数编号获取详细信息
     *
     * <p>url: GET /system/config/{configId}</p>
     *
     * @param url 请求路径
     * @param configId 参数主键
     * @return AjaxResult#success(SysConfig)
     */
    @PreAuthorize("@ss.hasPermi('system:config:query')")
    @GetMapping(value = "/{configId}")
    public AjaxResult getInfo(@PathVariable Long configId)
    {
        return success(configService.selectConfigById(configId));
    }

    /**
     * 根据参数键名查询参数值（公开接口）
     *
     * <p>url: GET /system/config/configKey/{configKey}</p>
     *
     * <p>未加 {@code @PreAuthorize}，匿名可访问。常用于前端或其他微服务读取配置（如验证码开关）。</p>
     *
     * @param url 请求路径
     * @param configKey 参数键名，如 {@code sys.account.captchaEnabled}
     * @return AjaxResult#success(String) 参数值
     */
    @GetMapping(value = "/configKey/{configKey}")
    public AjaxResult getConfigKey(@PathVariable String configKey)
    {
        return success(configService.selectConfigByKey(configKey));
    }

    /**
     * 新增参数配置
     *
     * <p>url: POST /system/config</p>
     *
     * <p>会校验 configKey 唯一性；自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param config 参数实体（经 {@code @Validated} 校验）
     * @return AjaxResult 成功/失败（失败时提示键名重复）
     */
    @PreAuthorize("@ss.hasPermi('system:config:add')")
    @Log(title = "参数管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysConfig config)
    {
        if (!configService.checkConfigKeyUnique(config))
        {
            return error("新增参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        config.setCreateBy(getUsername());
        return toAjax(configService.insertConfig(config));
    }

    /**
     * 修改参数配置
     *
     * <p>url: PUT /system/config</p>
     *
     * <p>会校验 configKey 唯一性；自动将当前操作用户写入 updateBy。</p>
     *
     * @param url 请求路径
     * @param config 参数实体（含 configId，经 {@code @Validated} 校验）
     * @return AjaxResult 成功/失败
     */
    @PreAuthorize("@ss.hasPermi('system:config:edit')")
    @Log(title = "参数管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysConfig config)
    {
        if (!configService.checkConfigKeyUnique(config))
        {
            return error("修改参数'" + config.getConfigName() + "'失败，参数键名已存在");
        }
        config.setUpdateBy(getUsername());
        return toAjax(configService.updateConfig(config));
    }

    /**
     * 批量删除参数配置
     *
     * <p>url: DELETE /system/config/{configIds}</p>
     *
     * @param url 请求路径
     * @param configIds 需要删除的参数主键数组
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @Log(title = "参数管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{configIds}")
    public AjaxResult remove(@PathVariable Long[] configIds)
    {
        configService.deleteConfigByIds(configIds);
        return success();
    }

    /**
     * 刷新 Redis 中的参数缓存
     *
     * <p>url: DELETE /system/config/refreshCache</p>
     *
     * <p>调用 {@code configService.resetConfigCache()}，重新从数据库全量加载参数到 Redis，
     * 用于参数变更后让业务立即生效。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('system:config:remove')")
    @Log(title = "参数管理", businessType = BusinessType.CLEAN)
    @DeleteMapping("/refreshCache")
    public AjaxResult refreshCache()
    {
        configService.resetConfigCache();
        return success();
    }
}
