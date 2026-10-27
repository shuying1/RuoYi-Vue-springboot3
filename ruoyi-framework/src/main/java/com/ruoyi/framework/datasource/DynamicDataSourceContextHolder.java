package com.ruoyi.framework.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 动态数据源上下文持有者 —— 基于 ThreadLocal 的数据源标识管理
 *
 * <p>设计意图：使用静态 {@link ThreadLocal} 存储当前线程指定的数据源名称，
 * 配合 {@link DataSourceAspect}（写入）和 {@link DynamicDataSource}（读取）
 * 实现声明式的多数据源切换。</p>
 *
 * <p>工作流程：</p>
 * <ol>
 *   <li>{@link DataSourceAspect#around} 在目标方法执行前调用 {@link #setDataSourceType} 将注解值写入 ThreadLocal</li>
 *   <li>{@link DynamicDataSource#determineCurrentLookupKey} 被 Spring JDBC 回调时，调用 {@link #getDataSourceType} 读取</li>
 *   <li>DataSourceAspect 在 finally 中调用 {@link #clearDataSourceType} 清理，防止线程池复用串用</li>
 * </ol>
 *
 * <p>线程安全：ThreadLocal 为每个请求线程提供独立的数据源标识副本，
 * 线程间互不干扰。但必须注意及时清理（尤其是在异步线程池场景）。</p>
 *
 * @author ruoyi
 * @see DataSourceAspect 数据源切换 AOP 切面（本类的写入者）
 * @see DynamicDataSource 动态数据源路由器（本类的主要读取者）
 */
public class DynamicDataSourceContextHolder
{
    /** 日志记录器，用于数据源切换时打印调试信息 */
    public static final Logger log = LoggerFactory.getLogger(DynamicDataSourceContextHolder.class);

    /**
     * 使用 ThreadLocal 维护变量，ThreadLocal 为每个使用该变量的线程提供独立的变量副本，
     * 所以每一个线程都可以独立地改变自己的副本，而不会影响其它线程所对应的副本。
     */
    private static final ThreadLocal<String> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的数据源标识。
     *
     * <p>通常由 {@link DataSourceAspect} 在方法执行前调用，
     * 参数值来自 {@link com.ruoyi.common.annotation.DataSource} 注解的 value 枚举名。</p>
     *
     * @param dsType 数据源类型字符串，如 "master"、"slave"
     */
    public static void setDataSourceType(String dsType)
    {
        log.info("切换到{}数据源", dsType);
        CONTEXT_HOLDER.set(dsType);
    }

    /**
     * 获取当前线程绑定的数据源标识。
     *
     * <p>由 {@link DynamicDataSource#determineCurrentLookupKey} 在每次需要获取 Connection 时调用。</p>
     *
     * @return 当前线程的数据源 Key；若未设置则返回 null
     */
    public static String getDataSourceType()
    {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 清空当前线程的数据源标识 —— 防止线程池复用时脏数据串用。
     *
     * <p>必须在方法执行完成后（通常在 finally 块中）调用。</p>
     */
    public static void clearDataSourceType()
    {
        CONTEXT_HOLDER.remove();
    }
}
