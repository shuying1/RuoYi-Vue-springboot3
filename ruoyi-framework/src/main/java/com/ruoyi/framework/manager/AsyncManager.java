package com.ruoyi.framework.manager;

import java.util.TimerTask;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.ruoyi.common.utils.Threads;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * 异步任务管理器 —— 单例模式的定时任务调度器
 *
 * <p>设计意图：采用懒汉式单例模式，封装 Spring 容器中的 {@code scheduledExecutorService}
 * 定时线程池，为整个应用提供统一的异步任务执行入口。
 * 主要用于操作日志、登录日志、通知消息等耗时 I/O 任务的异步执行，
 * 避免阻塞业务接口的响应线程。</p>
 *
 * <p>执行机制：</p>
 * <ul>
 *   <li>{@link #execute} —— 将 {@link TimerTask} 提交到定时线程池，延迟 {@link #OPERATE_DELAY_TIME}（10ms）执行</li>
 *   <li>延迟执行的目的：让任务在调用方请求响应后（或线程空闲时）再执行，减少对主业务线程的资源竞争</li>
 *   <li>线程池 Bean 定义在 {@link com.ruoyi.framework.config.ThreadPoolConfig#scheduledExecutorService}</li>
 * </ul>
 *
 * <p>在整体架构中的位置：</p>
 * <pre>{@code
 * 业务代码 (SysLoginService / LogAspect / ShutdownManager ...)
 *   ↓ AsyncManager.me().execute(TimerTask)
 * AsyncManager （单例）
 *   ↓ executor.schedule
 * ThreadPoolConfig#scheduledExecutorService （定时线程池）
 *   ↓ 执行具体任务
 * AsyncFactory#recordOper / recordLogininfor （异步工厂创建的 TimerTask）
 * }</pre>
 *
 * @author ruoyi
 * @see AsyncFactory 异步任务工厂（生产 TimerTask）
 * @see com.ruoyi.framework.config.ThreadPoolConfig#scheduledExecutorService 定时线程池 Bean 定义
 * @see ShutdownManager 应用关闭时调用本类 shutdown()
 */
public class AsyncManager
{
    /** 任务延迟执行时间（毫秒），10ms 用于让业务线程有机会先完成响应 */
    private final int OPERATE_DELAY_TIME = 10;

    /**
     * 定时任务调度线程池 Bean —— 从 Spring 容器中懒加载获取。
     *
     * <p>注意：由于 {@link AsyncManager} 不是 Spring Bean（通过 SpringUtils 间接获取），
     * 所以 executor 是懒加载的，首次调用 execute 时才从 Spring 容器获取。</p>
     */
    private ScheduledExecutorService executor = SpringUtils.getBean("scheduledExecutorService");

    /** 单例构造函数 —— 私有构造，防止外部实例化 */
    private AsyncManager(){}

    /** 单例实例 —— 类加载时即完成初始化（饿汉式） */
    private static AsyncManager me = new AsyncManager();

    /**
     * 获取单例实例。
     *
     * @return AsyncManager 单例
     */
    public static AsyncManager me()
    {
        return me;
    }

    /**
     * 异步执行一个 TimerTask。
     *
     * <p>任务会在 {@link #OPERATE_DELAY_TIME} 毫秒后被提交到定时线程池执行。</p>
     *
     * <p>典型调用示例：</p>
     * <pre>{@code
     * AsyncManager.me().execute(AsyncFactory.recordLogininfor("admin", Constants.LOGIN_SUCCESS, "登录成功"));
     * }</pre>
     *
     * @param task 待执行的定时任务（通常由 {@link AsyncFactory} 创建）
     */
    public void execute(TimerTask task)
    {
        executor.schedule(task, OPERATE_DELAY_TIME, TimeUnit.MILLISECONDS);
    }

    /**
     * 停止异步执行任务线程池 —— 应用关闭时调用。
     *
     * <p>通过 {@link Threads#shutdownAndAwaitTermination} 优雅关闭线程池：</p>
     * <ol>
     *   <li>不再接受新任务</li>
     *   <li>等待已提交任务执行完成（最长 1 秒）</li>
     *   <li>强制中断仍在执行的任务</li>
     * </ol>
     *
     * <p>由 {@link ShutdownManager#destroy} 在 JVM 关闭前调用（@PreDestroy）。</p>
     */
    public void shutdown()
    {
        Threads.shutdownAndAwaitTermination(executor);
    }
}
