package com.ruoyi.framework.web.domain;

import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import com.ruoyi.common.utils.Arith;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.web.domain.server.Cpu;
import com.ruoyi.framework.web.domain.server.Jvm;
import com.ruoyi.framework.web.domain.server.Mem;
import com.ruoyi.framework.web.domain.server.Sys;
import com.ruoyi.framework.web.domain.server.SysFile;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

/**
 * 服务器运行状态信息聚合实体 —— OSHI 采集 + 格式化输出
 *
 * <p>设计意图：基于开源系统监控库 OSHI（Operating System and Hardware Information）
 * 采集服务器的硬件和软件运行状态数据，包括 CPU、内存、JVM、操作系统、磁盘等，
 * 并聚合到单个 {@link Server} 实体中，通过 /monitor/server 接口以 JSON 格式返回给前端
 * 进行可视化展示。</p>
 *
 * <p>采集流程（{@link #copyTo}）：</p>
 * <ol>
 *   <li>初始化 OSHI 的 {@link SystemInfo} → {@link HardwareAbstractionLayer}</li>
 *   <li>{@link #setCpuInfo} —— 两次采样间隔 1 秒（{@link #OSHI_WAIT_SECOND}），
 *       计算 USER/SYSTEM/IOWAIT/IDLE 等各维度的 CPU 使用率</li>
 *   <li>{@link #setMemInfo} —— 读取 {@link GlobalMemory} 的 total/available</li>
 *   <li>{@link #setSysInfo} —— 读取系统属性（os.name / os.arch / user.dir）+ 主机名/IP</li>
 *   <li>{@link #setJvmInfo} —— 读取 {@link Runtime} 的 total/max/freeMemory + Java 版本/Home</li>
 *   <li>{@link #setSysFiles} —— 遍历 {@link OSFileStore} 列表，采集各磁盘的 total/free/used</li>
 * </ol>
 *
 * <p>字节转换（{@link #convertFileSize}）：自动在 B → KB → MB → GB 之间选择合适的单位，
 * 保持一位小数精度，超过 100 的整数不显示小数。</p>
 *
 * @author ruoyi
 * @see Cpu CPU 使用率详情
 * @see Mem 内存使用率详情
 * @see Jvm JVM 内存详情
 * @see Sys 操作系统信息
 * @see SysFile 磁盘分区信息
 * @see SystemInfo OSHI 系统信息入口
 */
public class Server
{
    /** OSHI CPU 使用率两次采样的间隔时间（毫秒）。
     *  不能太短（CPU tick 变化不明显），也不能太长（接口响应慢），1 秒为行业常规值。 */
    private static final int OSHI_WAIT_SECOND = 1000;
    
    /**
     * CPU 相关信息（核心数、使用率、系统/用户/等待/空闲率）。
     * 由 {@link #setCpuInfo} 填充，前端直接获取展示。
     */
    private Cpu cpu = new Cpu();

    /**
     * 内存相关信息（总量、已用、剩余）。
     * 由 {@link #setMemInfo} 填充，单位：GB。
     */
    private Mem mem = new Mem();

    /**
     * JVM 虚拟机相关信息（内存、版本、Home 路径、启动时间、运行时长、输入参数）。
     * 由 {@link #setJvmInfo} 填充。
     */
    private Jvm jvm = new Jvm();

    /**
     * 服务器操作系统信息（主机名、IP、OS 名称、架构、项目路径）。
     * 由 {@link #setSysInfo} 填充。
     */
    private Sys sys = new Sys();

    /**
     * 磁盘分区信息列表（每个分区的盘符、类型、总量、剩余、已用、使用率）。
     * 由 {@link #setSysFiles} 填充，按 LinkedList 顺序遍历。
     */
    private List<SysFile> sysFiles = new LinkedList<SysFile>();

    // ==================== Getter / Setter ====================

    public Cpu getCpu() { return cpu; }
    public void setCpu(Cpu cpu) { this.cpu = cpu; }

    public Mem getMem() { return mem; }
    public void setMem(Mem mem) { this.mem = mem; }

    public Jvm getJvm() { return jvm; }
    public void setJvm(Jvm jvm) { this.jvm = jvm; }

    public Sys getSys() { return sys; }
    public void setSys(Sys sys) { this.sys = sys; }

    public List<SysFile> getSysFiles() { return sysFiles; }
    public void setSysFiles(List<SysFile> sysFiles) { this.sysFiles = sysFiles; }

    /**
     * 从 OSHI 采集完整的服务器运行状态信息，填充到当前实体中。
     *
     * <p>这是外部调用的主入口，按顺序执行 CPU → 内存 → OS → JVM → 磁盘 五个采集步骤。</p>
     *
     * @throws Exception OSHI 采集过程中可能的异常（如 UnknownHostException）
     */
    public void copyTo() throws Exception
    {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        setCpuInfo(hal.getProcessor());

        setMemInfo(hal.getMemory());

        setSysInfo();

        setJvmInfo();

        setSysFiles(si.getOperatingSystem());
    }

    /**
     * 采集 CPU 各维度使用率 —— 两次采样法。
     *
     * <p>实现原理：先调用 getSystemCpuLoadTicks() 获得第 1 次 tick 快照，
     * sleep 1 秒后再获得第 2 次快照，
     * 两个快照的差值即为 1 秒内各维度的 CPU 消耗（USER / SYSTEM / IDLE / IOWAIT / IRQ / SOFTIRQ / STEAL / NICE），
     * 再按差值总和计算各维度百分比。</p>
     *
     * @param processor OSHI 提供的 CPU 信息对象
     */
    private void setCpuInfo(CentralProcessor processor)
    {
        // CPU信息
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(OSHI_WAIT_SECOND);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
        long cSys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        cpu.setCpuNum(processor.getLogicalProcessorCount());
        cpu.setTotal(totalCpu);
        cpu.setSys(cSys);
        cpu.setUsed(user);
        cpu.setWait(iowait);
        cpu.setFree(idle);
    }

    /**
     * 采集物理内存总量 / 已用 / 剩余。
     *
     * @param memory OSHI 提供的全局内存对象
     */
    private void setMemInfo(GlobalMemory memory)
    {
        mem.setTotal(memory.getTotal());
        mem.setUsed(memory.getTotal() - memory.getAvailable());
        mem.setFree(memory.getAvailable());
    }

    /**
     * 采集操作系统基础信息。
     *
     * <p>数据源：</p>
     * <ul>
     *   <li>主机名 / IP —— {@link IpUtils}</li>
     *   <li>OS 名称 / 架构 / 项目路径 —— {@link System#getProperties}</li>
     * </ul>
     */
    private void setSysInfo()
    {
        Properties props = System.getProperties();
        sys.setComputerName(IpUtils.getHostName());
        sys.setComputerIp(IpUtils.getHostIp());
        sys.setOsName(props.getProperty("os.name"));
        sys.setOsArch(props.getProperty("os.arch"));
        sys.setUserDir(props.getProperty("user.dir"));
    }

    /**
     * 采集 JVM 虚拟机运行时信息。
     *
     * <p>数据源：{@link Runtime}（内存）+ {@link System#getProperties}（版本 / Home）。</p>
     *
     * @throws UnknownHostException IP 解析异常（已由上层 copyTo 捕获）
     */
    private void setJvmInfo() throws UnknownHostException
    {
        Properties props = System.getProperties();
        jvm.setTotal(Runtime.getRuntime().totalMemory());
        jvm.setMax(Runtime.getRuntime().maxMemory());
        jvm.setFree(Runtime.getRuntime().freeMemory());
        jvm.setVersion(props.getProperty("java.version"));
        jvm.setHome(props.getProperty("java.home"));
    }

    /**
     * 采集磁盘分区列表及各分区使用率。
     *
     * <p>遍历 OSHI 返回的 {@link OSFileStore} 列表，
     * 对每个分区计算 used = total - free，并用 {@link Arith} 计算使用率百分比（保留 4 位小数）。</p>
     *
     * @param os OSHI 提供的操作系统对象
     */
    private void setSysFiles(OperatingSystem os)
    {
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray)
        {
            long free = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            long used = total - free;
            SysFile sysFile = new SysFile();
            sysFile.setDirName(fs.getMount());
            sysFile.setSysTypeName(fs.getType());
            sysFile.setTypeName(fs.getName());
            sysFile.setTotal(convertFileSize(total));
            sysFile.setFree(convertFileSize(free));
            sysFile.setUsed(convertFileSize(used));
            sysFile.setUsage(Arith.mul(Arith.div(used, total, 4), 100));
            sysFiles.add(sysFile);
        }
    }

    /**
     * 字节数自动转换为合适单位的字符串（B / KB / MB / GB）。
     *
     * <p>转换规则：</p>
     * <ul>
     *   <li>≥ GB → 显示 GB 并保留 1 位小数</li>
     *   <li>≥ MB → 显示 MB，值 > 100 时取整，否则保留 1 位小数</li>
     *   <li>≥ KB → 显示 KB，值 > 100 时取整，否则保留 1 位小数</li>
     *   <li>否则 → 显示 B（整数）</li>
     * </ul>
     *
     * @param size 原始字节数
     * @return 格式化后的字符串（如 "2.3 GB"、"128.5 MB"、"4096 KB"）
     */
    public String convertFileSize(long size)
    {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb)
        {
            return String.format("%.1f GB", (float) size / gb);
        }
        else if (size >= mb)
        {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        }
        else if (size >= kb)
        {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        }
        else
        {
            return String.format("%d B", size);
        }
    }
}
