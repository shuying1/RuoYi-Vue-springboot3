package com.ruoyi.framework.web.domain.server;

import java.lang.management.ManagementFactory;
import com.ruoyi.common.utils.Arith;
import com.ruoyi.common.utils.DateUtils;

/**
 * JVM 虚拟机运行时信息
 *
 * <p>设计意图：封装 Java 虚拟机的内存、版本、Home 路径、启动时间、运行时长和输入参数等信息，
 * 用于监控后端 Java 进程的运行状态（内存使用是否合理、JVM 是否长时间运行等）。</p>
 *
 * <p>数据来源：{@link Runtime}（内存）、{@link ManagementFactory#getRuntimeMXBean}（VM 名称 / 启动时间 / 输入参数）、
 * {@link System#getProperties}（Java 版本 / Home）。</p>
 *
 * <p>内存字段单位：setter 接收字节数，getter 自动转换为 MB（÷ 1024 × 1024）并保留 2 位小数。</p>
 *
 * @author ruoyi
 * @see com.ruoyi.framework.web.domain.Server#setJvmInfo 本类数据的填充入口
 */
public class Jvm
{
    /** JVM 当前已占用内存（字节），getter 转换为 MB */
    private double total;

    /** JVM 最大可用内存（字节），由 -Xmx 参数指定，getter 转换为 MB */
    private double max;

    /** JVM 空闲内存（字节），getter 转换为 MB */
    private double free;

    /** JDK 版本号（如 "17.0.8"、"11.0.20"） */
    private String version;

    /** JDK 安装根目录（如 "/usr/lib/jvm/java-17-openjdk-amd64"） */
    private String home;

    /** JVM 已用内存（MB）= total - free */
    public double getTotal() { return Arith.div(total, (1024 * 1024), 2); }
    public void setTotal(double total) { this.total = total; }

    /** JVM 最大内存（MB） */
    public double getMax() { return Arith.div(max, (1024 * 1024), 2); }
    public void setMax(double max) { this.max = max; }

    /** JVM 空闲内存（MB） */
    public double getFree() { return Arith.div(free, (1024 * 1024), 2); }
    public void setFree(double free) { this.free = free; }

    /** JVM 已使用内存（MB）= total - free */
    public double getUsed() { return Arith.div(total - free, (1024 * 1024), 2); }

    /** JVM 内存使用率百分比 = (total - free) / total × 100 */
    public double getUsage() { return Arith.mul(Arith.div(total - free, total, 4), 100); }

    /**
     * 获取 JVM 实现名称（如 "OpenJDK 64-Bit Server VM"）。
     *
     * @return JVM 虚拟机名，由 ManagementFactory 提供
     */
    public String getName() { return ManagementFactory.getRuntimeMXBean().getVmName(); }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getHome() { return home; }
    public void setHome(String home) { this.home = home; }

    /**
     * 获取 JVM 启动时间（格式化字符串）。
     *
     * <p>由 {@link DateUtils#getServerStartDate} 获取 ServerSocket 的绑定时间作为近似值，
     * 再格式化为 {@code yyyy-MM-dd HH:mm:ss} 格式。</p>
     *
     * @return JVM 启动时间字符串
     */
    public String getStartTime() { return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, DateUtils.getServerStartDate()); }

    /**
     * 获取 JVM 运行时长（人类可读格式）。
     *
     * <p>计算方式：当前时间 - ServerSocket 启动时间，
     * 由 {@link DateUtils#timeDistance} 格式化为 "X天XX小时XX分钟XX秒"。</p>
     *
     * @return 运行时长字符串
     */
    public String getRunTime() { return DateUtils.timeDistance(DateUtils.getNowDate(), DateUtils.getServerStartDate()); }

    /**
     * 获取 JVM 启动时的输入参数列表（如 -Xms512m -Xmx2048m -Dfile.encoding=UTF-8 等）。
     *
     * @return 参数列表的字符串格式（toString 输出）
     */
    public String getInputArgs() { return ManagementFactory.getRuntimeMXBean().getInputArguments().toString(); }
}
