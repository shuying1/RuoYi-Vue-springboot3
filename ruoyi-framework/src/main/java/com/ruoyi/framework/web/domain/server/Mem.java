package com.ruoyi.framework.web.domain.server;

import com.ruoyi.common.utils.Arith;

/**
 * 物理内存使用率信息
 *
 * <p>设计意图：封装服务器物理内存的总量、已用、剩余三个核心指标，
 * 由 {@link com.ruoyi.framework.web.domain.Server#setMemInfo} 通过 OSHI 填充原始字节数，
 * getter 自动转换为 GB（÷ 1024³）并保留 2 位小数。</p>
 *
 * <p>与 JVM 内存的区别：
 * 本类描述操作系统级物理内存（所有进程共享），
 * {@link Jvm} 描述 Java 进程独占的堆内存 + 元空间。</p>
 *
 * @author ruoyi
 * @see Jvm JVM 内存信息（本类的"邻居"）
 */
public class Mem
{
    /** 物理内存总量（字节，setter 接收），getter 转换为 GB */
    private double total;

    /** 已使用内存（字节，setter 接收），getter 转换为 GB */
    private double used;

    /** 剩余可用内存（字节，setter 接收），getter 转换为 GB */
    private double free;

    /** 物理内存总量（GB，保留 2 位小数） */
    public double getTotal() { return Arith.div(total, (1024 * 1024 * 1024), 2); }
    public void setTotal(long total) { this.total = total; }

    /** 已使用内存（GB） */
    public double getUsed() { return Arith.div(used, (1024 * 1024 * 1024), 2); }
    public void setUsed(long used) { this.used = used; }

    /** 剩余可用内存（GB） */
    public double getFree() { return Arith.div(free, (1024 * 1024 * 1024), 2); }
    public void setFree(long free) { this.free = free; }

    /** 内存使用率百分比 = used / total × 100，保留 2 位小数 */
    public double getUsage() { return Arith.mul(Arith.div(used, total, 4), 100); }
}
