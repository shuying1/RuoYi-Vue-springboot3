package com.ruoyi.web.controller.monitor;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.framework.web.domain.Server;

/**
 * 服务器监控 Controller
 *
 * <p>业务模块：系统监控 — 服务器运行指标，路由前缀 {@code /monitor/server}。
 * 本控制器直接实例化 {@link Server} 并调用 {@code copyTo()} 采集指标，
 * 不依赖 Service 层。覆盖 CPU、内存、JVM、磁盘、系统信息五维度。</p>
 *
 * <p>依赖类：{@link Server}（采集 JVM/OS/Disk 指标的工具类，位于 framework 模块）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/server")
public class ServerController
{
    /**
     * 获取服务器运行指标快照
     *
     * <p>url: GET /monitor/server</p>
     *
     * <p>返回 data 结构：</p>
     * <pre>
     * {
     *   "cpu":      Cpu,          // CPU 使用率、核心数、系统/用户负载
     *   "mem":      Mem,          // 物理内存总量、已用、剩余
     *   "jvm":      Jvm,          // JVM 版本、内存使用、GC 统计、启动时长
     *   "sys":      Sys,          // 操作系统名称、IP、架构
     *   "sysFiles": [SysFile, ...] // 磁盘分区使用情况
     * }
     * </pre>
     *
     * @param url 请求路径（GET /monitor/server）
     * @return AjaxResult 成功时携带 {@link Server} 对象
     * @throws Exception 指标采集过程中的 IO / 反射异常
     */
    @PreAuthorize("@ss.hasPermi('monitor:server:list')")
    @GetMapping()
    public AjaxResult getInfo() throws Exception
    {
        Server server = new Server();
        server.copyTo();
        return AjaxResult.success(server);
    }
}
