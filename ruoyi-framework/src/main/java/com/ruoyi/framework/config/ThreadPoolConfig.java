package com.ruoyi.framework.config;

import com.ruoyi.common.utils.Threads;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 线程池配置 —— 通用异步线程池 + 定时任务线程池
 *
 * <p>设计意图：为系统提供两个独立的线程池 Bean：</p>
 * <ul>
 *   <li><b>threadPoolTaskExecutor</b> —— 通用异步任务线程池，供 {@link AsyncManager} 执行日志记录、通知等异步任务</li>
 *   <li><b>scheduledExecutorService</b> —— 定时 / 周期性任务线程池，支持 ScheduledFuture 调度</li>
 * </ul>
 *
 * <p>拒绝策略：统一使用 {@link ThreadPoolExecutor.CallerRunsPolicy}，
 * 即队列满时由调用线程（如 Tomcat 工作线程）自己执行任务，避免任务丢失。</p>
 *
 * <p>线程池参数说明：</p>
 * <ul>
 *   <li>corePoolSize = 50 —— 核心线程数（IO 密集型 2N 公式，N 为 CPU 核数，默认 8 核）</li>
 *   <li>maxPoolSize = 200 —— 最大线程数</li>
 *   <li>queueCapacity = 1000 —— 阻塞队列容量（LinkedBlockingQueue）</li>
 *   <li>keepAliveSeconds = 300 —— 非核心线程空闲超时时间（秒）</li>
 * </ul>
 *
 * @author ruoyi
 * @see ThreadPoolTaskExecutor Spring 封装的线程池
 * @see ScheduledThreadPoolExecutor JDK 定时线程池
 * @see AsyncManager 使用 threadPoolTaskExecutor 执行异步日志写入
 */
@Configuration
public class ThreadPoolConfig
{
    /** 核心线程池大小（默认 50），IO 密集型应用 2N+2 推荐值 */
    private int corePoolSize = 50;

    /** 最大可创建的线程数（默认 200），受系统资源限制，生产环境建议根据服务器配置调整 */
    private int maxPoolSize = 200;

    /** 队列最大长度（默认 1000），线程池拒绝前可缓冲的任务数 */
    private int queueCapacity = 1000;

    /** 线程池维护线程所允许的空闲时间（秒，默认 300），超过此时间的非核心线程会被回收 */
    private int keepAliveSeconds = 300;

    /**
     * 通用异步任务线程池 Bean。
     *
     * <p>主要用途：{@link AsyncManager} 异步执行操作日志、登录日志、通知消息等耗时任务。
     * 拒绝策略为 CallerRunsPolicy，保证任务不丢失（队列满时降级为同步执行）。</p>
     *
     * @return 配置好的 ThreadPoolTaskExecutor 实例
     */
    @Bean(name = "threadPoolTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor()
    {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(maxPoolSize);
        executor.setCorePoolSize(corePoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        // 线程池对拒绝任务(无线程可用)的处理策略 —— 由调用线程自己执行，避免任务丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }

    /**
     * 定时 / 周期性任务线程池 Bean。
     *
     * <p>特性：</p>
     * <ul>
     *   <li>线程命名模式 {@code schedule-pool-%d}，便于线程堆栈排查</li>
     *   <li>daemon 线程，JVM 关闭时自动终止</li>
     *   <li>重写 {@link ScheduledThreadPoolExecutor#afterExecute} 统一打印任务异常堆栈（通过 {@link Threads#printException}）</li>
     * </ul>
     *
     * @return ScheduledExecutorService 实例
     */
    @Bean(name = "scheduledExecutorService")
    protected ScheduledExecutorService scheduledExecutorService()
    {
        return new ScheduledThreadPoolExecutor(corePoolSize,
                new BasicThreadFactory.Builder().namingPattern("schedule-pool-%d").daemon(true).build(),
                new ThreadPoolExecutor.CallerRunsPolicy())
        {
            @Override
            protected void afterExecute(Runnable r, Throwable t)
            {
                super.afterExecute(r, t);
                // 统一拦截线程执行后的异常并打印堆栈（避免定时任务异常静默丢失）
                Threads.printException(r, t);
            }
        };
    }
}
