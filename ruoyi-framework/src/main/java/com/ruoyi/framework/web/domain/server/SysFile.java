package com.ruoyi.framework.web.domain.server;

/**
 * 磁盘分区信息
 *
 * <p>设计意图：封装单个磁盘分区（Windows 下是盘符 C:/D:/，Linux 下是挂载点 /、/home 等）
 * 的类型、名称、总量、剩余、已用和使用率。</p>
 *
 * <p>数据来源：由 {@link com.ruoyi.framework.web.domain.Server#setSysFiles} 通过 OSHI 遍历
 * {@link oshi.software.os.OSFileStore} 列表填充。
 * 总 / 已用 / 剩余三个字段已由 {@link com.ruoyi.framework.web.domain.Server#convertFileSize}
 * 格式化为带单位的字符串（如 "512.0 GB"）。</p>
 *
 * @author ruoyi
 * @see com.ruoyi.framework.web.domain.Server#convertFileSize 字节自动转换工具
 */
public class SysFile
{
    /** 分区挂载点（Windows 如 "C:\\"，Linux 如 "/"、"/home"） */
    private String dirName;

    /** 文件系统类型（如 "NTFS"、"ext4"、"xfs"） */
    private String sysTypeName;

    /** 文件系统名称（卷标或设备名，如 "Local Disk"、"/dev/sda1"） */
    private String typeName;

    /** 分区总量（已格式化，如 "512.0 GB"） */
    private String total;

    /** 分区剩余可用空间（已格式化，如 "256.3 GB"） */
    private String free;

    /** 分区已使用空间（已格式化，如 "255.7 GB"） */
    private String used;

    /** 分区使用率百分比（0.0 ~ 100.0，保留 2 位小数） */
    private double usage;

    public String getDirName() { return dirName; }
    public void setDirName(String dirName) { this.dirName = dirName; }

    public String getSysTypeName() { return sysTypeName; }
    public void setSysTypeName(String sysTypeName) { this.sysTypeName = sysTypeName; }

    public String getTypeName() { return typeName; }
    public void setTypeName(String typeName) { this.typeName = typeName; }

    public String getTotal() { return total; }
    public void setTotal(String total) { this.total = total; }

    public String getFree() { return free; }
    public void setFree(String free) { this.free = free; }

    public String getUsed() { return used; }
    public void setUsed(String used) { this.used = used; }

    public double getUsage() { return usage; }
    public void setUsage(double usage) { this.usage = usage; }
}
