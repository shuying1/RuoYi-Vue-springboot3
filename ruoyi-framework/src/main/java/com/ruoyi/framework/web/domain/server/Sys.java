package com.ruoyi.framework.web.domain.server;

/**
 * 操作系统基础信息
 *
 * <p>设计意图：封装服务器的操作系统名称、架构、主机名、IP 和项目工作目录，
 * 用于前端服务器监控页面展示基础信息面板。</p>
 *
 * <p>数据来源：由 {@link com.ruoyi.framework.web.domain.Server#setSysInfo} 填充，
 * 包括 {@link IpUtils}（主机名/IP）和 {@link System#getProperties}（os.name / os.arch / user.dir）。</p>
 *
 * @author ruoyi
 * @see com.ruoyi.framework.web.domain.Server#setSysInfo 本类数据的填充入口
 */
public class Sys
{
    /** 服务器主机名（如 "DESKTOP-A1B2C3D"、"server-01"） */
    private String computerName;

    /** 服务器内网 IP 地址（如 "192.168.1.100"、"10.0.0.5"） */
    private String computerIp;

    /** Java 进程的工作目录（user.dir，通常是项目根路径或 JAR 所在目录） */
    private String userDir;

    /** 操作系统名称（如 "Windows 11"、"Linux"、"Mac OS X"） */
    private String osName;

    /** 系统架构（如 "amd64"、"aarch64"） */
    private String osArch;

    public String getComputerName() { return computerName; }
    public void setComputerName(String computerName) { this.computerName = computerName; }

    public String getComputerIp() { return computerIp; }
    public void setComputerIp(String computerIp) { this.computerIp = computerIp; }

    public String getUserDir() { return userDir; }
    public void setUserDir(String userDir) { this.userDir = userDir; }

    public String getOsName() { return osName; }
    public void setOsName(String osName) { this.osName = osName; }

    public String getOsArch() { return osArch; }
    public void setOsArch(String osArch) { this.osArch = osArch; }
}
