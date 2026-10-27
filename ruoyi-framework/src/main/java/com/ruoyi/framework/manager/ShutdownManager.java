package com.ruoyi.framework.manager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import jakarta.annotation.PreDestroy;

/**
 * 应用关闭钩子 —— 确保后台线程优雅终止
 *
 * <p>设计意图：利用 Spring 的 {@link PreDestroy} 生命周期钩子，
 * 在 ApplicationContext 关闭（JVM 退出 / Tomcat 停止）时，
 * 主动调用 {@link AsyncManager#shutdown} 优雅关闭定时任务线程池，
 * 避免后台任务被强制中断导致日志丢失、资源泄漏等问题。</p>
 *
 * <p>执行时机：Spring 容器销毁阶段，所有 Service Bean 的 @PreDestroy 方法都会被调用。
 * 此方法必须快速返回，避免阻塞 JVM 关闭流程。</p>
 *
 * <p>与 {@link AsyncManager} 的关系：
 * 本类是 {@link AsyncManager#shutdown} 的唯一调用入口，
 * 保证应用退出时异步线程池被正确关闭。</p>
 *
 * @author ruoyi
 * @see AsyncManager#shutdown 被本类调用的异步线程池关闭方法
 * @see PreDestroy Spring Bean 销毁回调注解
 */
@Component
public class ShutdownManager
{
    /** sys-user 专用 Logger，与 AsyncFactory 共用同一个日志文件 */
    private static final Logger logger = LoggerFactory.getLogger("sys-user");

    /**
     * Spring 容器销毁回调 —— 关闭异步任务线程池。
     *
     * <p>此方法会被 Spring 在应用关闭时自动触发（ContextClosedEvent）。
     * 内部调用 {@link #shutdownAsyncManager}，其本身不做业务逻辑，
     * 仅作为 Spring 生命周期钩子的入口。</p>
     */
    @PreDestroy
    public void destroy()
    {
        shutdownAsyncManager();
    }

    /**
     * 优雅停止异步执行任务线程池（内部方法）。
     *
     * <p>异常被 catch 后仅打日志，不影响 JVM 关闭流程。</p>
     */
    private void shutdownAsyncManager()
    {
        try
        {
            logger.info("====关闭后台任务任务线程池====");
            AsyncManager.me().shutdown();
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
        }
    }
}
