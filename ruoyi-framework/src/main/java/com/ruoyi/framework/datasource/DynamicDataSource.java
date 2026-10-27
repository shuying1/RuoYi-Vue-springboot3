package com.ruoyi.framework.datasource;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * 动态数据源路由器 —— 基于 Spring AbstractRoutingDataSource 的多数据源实现
 *
 * <p>设计意图：继承 Spring JDBC 的 {@link AbstractRoutingDataSource}，
 * 实现 {@link #determineCurrentLookupKey} 抽象方法，
 * 从 {@link DynamicDataSourceContextHolder} 的 ThreadLocal 中读取当前线程指定的数据源 Key，
 * 从而将 JDBC 操作路由到对应的物理数据源（master / slave 等）。</p>
 *
 * <p>切换策略：</p>
 * <ol>
 *   <li>{@link DataSourceAspect} 切面在目标方法执行前，通过注解将数据源标识写入 ThreadLocal</li>
 *   <li>Spring 自动调用本类的 {@link #determineCurrentLookupKey}，返回该标识</li>
 *   <li>父类 {@link AbstractRoutingDataSource} 根据返回的 Key 从 targetDataSources Map 中查找真实 DataSource</li>
 *   <li>方法执行后，{@link DataSourceAspect} 在 finally 中清理 ThreadLocal</li>
 * </ol>
 *
 * <p>构造函数会在 Spring Boot 自动配置（通常是 DruidConfig）中被调用，
 * 传入默认数据源（master）和所有目标数据源的 Map。</p>
 *
 * @author ruoyi
 * @see AbstractRoutingDataSource Spring 提供的路由数据源抽象基类
 * @see DynamicDataSourceContextHolder 数据源标识的 ThreadLocal 持有者
 * @see DataSourceAspect 数据源切换 AOP 切面（负责在方法级设置 ThreadLocal）
 */
public class DynamicDataSource extends AbstractRoutingDataSource
{
    /**
     * 构造动态数据源路由器。
     *
     * <p>设置默认目标数据源（当 ThreadLocal 中没有指定时使用）和所有候选数据源 Map，
     * 并调用 {@link #afterPropertiesSet} 初始化父类的 resolvedDefaultDataSource 和 resolvedDataSources。</p>
     *
     * @param defaultTargetDataSource 默认数据源（通常是 master 主库），当 ContextHolder 中没有设置 Key 时使用
     * @param targetDataSources 所有目标数据源的映射表，Key 为数据源标识（如 "master"、"slave"），Value 为物理 DataSource Bean
     */
    public DynamicDataSource(DataSource defaultTargetDataSource, Map<Object, Object> targetDataSources)
    {
        super.setDefaultTargetDataSource(defaultTargetDataSource);
        super.setTargetDataSources(targetDataSources);
        // 触发父类初始化，将 targetDataSources Map 转换为 resolvedDataSources（用于运行时快速查找）
        super.afterPropertiesSet();
    }

    /**
     * 核心路由方法 —— 获取当前线程的数据源标识。
     *
     * <p>Spring JDBC 在每次需要获取 Connection 时都会调用此方法，
     * 返回值即为之前 {@link DataSourceAspect} 通过 {@link DynamicDataSourceContextHolder#setDataSourceType} 设置的 Key。</p>
     *
     * @return 当前线程的数据源 Key（如 "master" / "slave"）；若 ThreadLocal 为空则返回 null，
     *         此时父类会使用 defaultTargetDataSource（主库）
     */
    @Override
    protected Object determineCurrentLookupKey()
    {
        return DynamicDataSourceContextHolder.getDataSourceType();
    }
}