package com.ruoyi.framework.manager.factory;

import java.util.TimerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.utils.LogUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.UserAgentUtils;
import com.ruoyi.common.utils.ip.AddressUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.system.domain.SysLogininfor;
import com.ruoyi.system.domain.SysOperLog;
import com.ruoyi.system.service.ISysLogininforService;
import com.ruoyi.system.service.ISysOperLogService;

/**
 * 异步任务工厂 —— 生产可交给 {@link AsyncManager} 执行的 TimerTask
 *
 * <p>设计意图：静态工厂模式，提供两类异步任务的创建方法：</p>
 * <ul>
 *   <li>{@link #recordLogininfor} —— 登录/登出/注册日志的异步写入</li>
 *   <li>{@link #recordOper} —— 操作日志的异步写入</li>
 * </ul>
 *
 * <p>为什么需要工厂？
 * 业务代码（如 {@link com.ruoyi.framework.aspectj.LogAspect}）只关心"我想异步写一条日志"，
 * 不需要关心如何构造 SysLogininfor / SysOperLog 对象、如何获取用户 IP 和地理位置、
 * 如何从 Spring 容器获取 Service Bean、如何处理异常等 —— 这些逻辑全部封装在本工厂中。</p>
 *
 * <p>工厂产出的 TimerTask 特征：</p>
 * <ul>
 *   <li>在 Task 被创建时就采集了请求上下文（IP、User-Agent、当前地址等），
 *       避免任务执行时（请求线程已结束）再去获取上下文失败</li>
 *   <li>通过 {@link SpringUtils#getBean} 从容器获取 Service Bean，
 *       因为 TimerTask 本身不是 Spring Bean 无法直接注入</li>
 *   <li>内部异常全部 catch 并打日志，不向上抛出（防止异步线程异常丢失导致线程池异常中断）</li>
 * </ul>
 *
 * @author ruoyi
 * @see AsyncManager#execute(TimerTask) 本工厂产出的任务交给此管理器执行
 * @see com.ruoyi.framework.aspectj.LogAspect 使用本工厂创建操作日志异步任务
 */
public class AsyncFactory
{
    /** 系统用户日志专用 Logger（org.slf4j.Logger），用于在异步任务执行时打印结构化日志到专用文件 */
    private static final Logger sys_user_logger = LoggerFactory.getLogger("sys-user");

    /**
     * 创建一条登录/登出/注册日志的异步写库任务。
     *
     * <p>在 Task 构造时就采集请求上下文：</p>
     * <ul>
     *   <li>从 ServletUtils 获取 User-Agent 头 → 后续解析浏览器和操作系统</li>
     *   <li>从 IpUtils 获取客户端 IP → 后续解析地理位置</li>
     * </ul>
     *
     * <p>Task 执行时（延迟 10ms 后）：</p>
     * <ol>
     *   <li>通过 AddressUtils 反查 IP 地理位置</li>
     *   <li>通过 UserAgentUtils 解析浏览器和 OS</li>
     *   <li>根据 status 判断是成功还是失败，写入 SysLogininfor.status</li>
     *   <li>通过 SpringUtils.getBean 获取 ISysLogininforService 并写入数据库</li>
     * </ol>
     *
     * @param username 登录用户名（SysUser.username）
     * @param status 登录状态（Constants.LOGIN_SUCCESS / LOGIN_FAIL / LOGOUT / REGISTER）
     * @param message 状态说明消息（通常是国际化 key 对应的文案）
     * @param args 日志占位符参数（可选，用于 sys_user_logger 的格式化输出）
     * @return 可提交给 {@link AsyncManager#execute} 的 TimerTask 实例
     */
    public static TimerTask recordLogininfor(final String username, final String status, final String message,
            final Object... args)
    {
        final String userAgent = ServletUtils.getRequest().getHeader("User-Agent");
        final String ip = IpUtils.getIpAddr();
        return new TimerTask()
        {
            @Override
            public void run()
            {
                String address = AddressUtils.getRealAddressByIP(ip);
                StringBuilder s = new StringBuilder();
                s.append(LogUtils.getBlock(ip));
                s.append(address);
                s.append(LogUtils.getBlock(username));
                s.append(LogUtils.getBlock(status));
                s.append(LogUtils.getBlock(message));
                // 打印信息到日志
                sys_user_logger.info(s.toString(), args);
                // 获取客户端操作系统
                String os = UserAgentUtils.getOperatingSystem(userAgent);
                // 获取客户端浏览器
                String browser = UserAgentUtils.getBrowser(userAgent);
                // 封装对象
                SysLogininfor logininfor = new SysLogininfor();
                logininfor.setUserName(username);
                logininfor.setIpaddr(ip);
                logininfor.setLoginLocation(address);
                logininfor.setBrowser(browser);
                logininfor.setOs(os);
                logininfor.setMsg(message);
                // 日志状态
                if (StringUtils.equalsAny(status, Constants.LOGIN_SUCCESS, Constants.LOGOUT, Constants.REGISTER))
                {
                    logininfor.setStatus(Constants.SUCCESS);
                }
                else if (Constants.LOGIN_FAIL.equals(status))
                {
                    logininfor.setStatus(Constants.FAIL);
                }
                // 插入数据
                SpringUtils.getBean(ISysLogininforService.class).insertLogininfor(logininfor);
            }
        };
    }

    /**
     * 创建一条操作日志的异步写库任务。
     *
     * <p>与 {@link #recordLogininfor} 不同：本方法不预先采集 User-Agent，
     * 因为 {@link SysOperLog} 对象在 Task 创建时已经由 {@link com.ruoyi.framework.aspectj.LogAspect}
     * 完整填充（包括 operIp / operName / operUrl / costTime / jsonResult 等）。</p>
     *
     * <p>Task 执行时仅需补充一项：
     * 通过 AddressUtils 反查 operIp 的地理位置并回填到 operLocation 字段。</p>
     *
     * @param operLog 已由调用方填充好的操作日志实体
     * @return 可提交给 {@link AsyncManager#execute} 的 TimerTask 实例
     */
    public static TimerTask recordOper(final SysOperLog operLog)
    {
        return new TimerTask()
        {
            @Override
            public void run()
            {
                // 远程查询操作地点
                operLog.setOperLocation(AddressUtils.getRealAddressByIP(operLog.getOperIp()));
                SpringUtils.getBean(ISysOperLogService.class).insertOperlog(operLog);
            }
        };
    }
}
