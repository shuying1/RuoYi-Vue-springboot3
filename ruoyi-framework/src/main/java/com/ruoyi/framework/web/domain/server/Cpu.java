package com.ruoyi.framework.web.domain.server;

import com.ruoyi.common.utils.Arith;

/**
 * CPU 使用率信息
 *
 * <p>设计意图：封装 CPU 的核心硬件信息（核心数）和各维度使用率：
 * total（总使用率）、sys（系统态）、used（用户态）、wait（IO 等待）、free（空闲）。
 * 所有百分比 getter 内部会做归一化处理：先除以 total 得到占比，再乘以 100 转为百分比并保留 2 位小数。</p>
 *
 * <p>数据来源：由 {@link com.ruoyi.framework.web.domain.Server#setCpuInfo}
 * 通过 OSHI 库采样填充原始 tick 差值（setter），getter 负责计算百分比。</p>
 *
 * @author ruoyi
 * @see com.ruoyi.framework.web.domain.Server 本类的填充者
 */
public class Cpu
{
    /** CPU 逻辑核心数（物理核数 × 超线程系数） */
    private int cpuNum;

    /**
     * CPU 总使用率（原始 tick 差值）。
     * getter 内部会乘以 100 得到百分比并保留 2 位小数。
     */
    private double total;

    /**
     * CPU 系统态使用率（原始 tick 差值）。
     * getter 内部会除以 total 再乘以 100。
     */
    private double sys;

    /**
     * CPU 用户态使用率（原始 tick 差值）。
     * getter 内部会除以 total 再乘以 100。
     */
    private double used;

    /**
     * CPU IO 等待率（原始 tick 差值）。
     * getter 内部会除以 total 再乘以 100。
     */
    private double wait;

    /**
     * CPU 空闲率（原始 tick 差值）。
     * getter 内部会除以 total 再乘以 100。
     */
    private double free;

    public int getCpuNum() { return cpuNum; }
    public void setCpuNum(int cpuNum) { this.cpuNum = cpuNum; }

    /** CPU 总使用率百分比（0~100，保留 2 位小数） */
    public double getTotal() { return Arith.round(Arith.mul(total, 100), 2); }
    public void setTotal(double total) { this.total = total; }

    /** CPU 系统态使用率百分比 */
    public double getSys() { return Arith.round(Arith.mul(sys / total, 100), 2); }
    public void setSys(double sys) { this.sys = sys; }

    /** CPU 用户态使用率百分比 */
    public double getUsed() { return Arith.round(Arith.mul(used / total, 100), 2); }
    public void setUsed(double used) { this.used = used; }

    /** CPU IO 等待率百分比 */
    public double getWait() { return Arith.round(Arith.mul(wait / total, 100), 2); }
    public void setWait(double wait) { this.wait = wait; }

    /** CPU 空闲率百分比 */
    public double getFree() { return Arith.round(Arith.mul(free / total, 100), 2); }
    public void setFree(double free) { this.free = free; }
}
