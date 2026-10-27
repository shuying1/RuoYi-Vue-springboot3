package com.ruoyi.framework.config.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import com.alibaba.druid.pool.DruidDataSource;

/**
 * Druid 连接池通用属性配置 —— 为多数据源统一补充连接池参数
 *
 * <p>设计意图：将所有数据源共用的连接池参数（initialSize、maxActive、maxWait、
 * 超时时间、连接检测策略等）从 master/slave 具体数据源配置中抽取出来，
 * 集中管理。通过 {@link #dataSource} 方法统一应用到每个 DruidDataSource Bean 上。</p>
 *
 * <p>使用场景：在 {@link com.ruoyi.framework.config.DruidConfig#masterDataSource}
 * 和 {@code slaveDataSource} Bean 创建后，调用本类的 {@link #dataSource} 方法
 * 将通用属性注入进去，避免在配置文件中重复编写。</p>
 *
 * <p>连接池参数详解：</p>
 * <ul>
 *   <li><b>连接数量</b>：initialSize（初始化连接数）、minIdle（最小空闲连接）、maxActive（最大活跃连接）</li>
 *   <li><b>等待超时</b>：maxWait（从连接池获取连接的最大等待时间）</li>
 *   <li><b>网络超时</b>：connectTimeout（建立连接超时）、socketTimeout（SQL 执行网络超时）</li>
 *   <li><b>空闲检测</b>：timeBetweenEvictionRunsMillis（检测间隔）、
 *       minEvictableIdleTimeMillis / maxEvictableIdleTimeMillis（连接最小/最大空闲时间）</li>
 *   <li><b>健康检查</b>：validationQuery（连接有效性 SQL，通常为 {@code SELECT 1} 或 {@code SELECT 'x'}）、
 *       testWhileIdle（空闲时检测，推荐）、testOnBorrow（借出时检测，影响性能）、testOnReturn（归还时检测，影响性能）</li>
 * </ul>
 *
 * @author ruoyi
 * @see com.ruoyi.framework.config.DruidConfig 使用本类配置 master/slave 数据源
 */
@Configuration
public class DruidProperties
{
    /** 初始化连接数 —— 连接池启动时预创建的连接数量 */
    @Value("${spring.datasource.druid.initialSize}")
    private int initialSize;

    /** 最小空闲连接数 —— 连接池始终维持的最小可用连接 */
    @Value("${spring.datasource.druid.minIdle}")
    private int minIdle;

    /** 最大活跃连接数 —— 连接池上限（超过时新请求需等待） */
    @Value("${spring.datasource.druid.maxActive}")
    private int maxActive;

    /** 获取连接的最大等待时间（毫秒） —— 连接池满时，新请求最多等待多久 */
    @Value("${spring.datasource.druid.maxWait}")
    private int maxWait;

    /** 建立数据库连接的超时时间（毫秒） —— 用于检测数据库可达性 */
    @Value("${spring.datasource.druid.connectTimeout}")
    private int connectTimeout;

    /** SQL 执行的网络超时时间（毫秒） —— 防止慢查询长时间占用连接 */
    @Value("${spring.datasource.druid.socketTimeout}")
    private int socketTimeout;

    /** 空闲连接检测间隔（毫秒） —— Druid 后台线程定期检查连接有效性的频率 */
    @Value("${spring.datasource.druid.timeBetweenEvictionRunsMillis}")
    private int timeBetweenEvictionRunsMillis;

    /** 连接在池中可空闲的最小时间（毫秒） —— 低于此时间的空闲连接不会被回收 */
    @Value("${spring.datasource.druid.minEvictableIdleTimeMillis}")
    private int minEvictableIdleTimeMillis;

    /** 连接在池中可空闲的最大时间（毫秒） —— 超过此时间的空闲连接会被回收 */
    @Value("${spring.datasource.druid.maxEvictableIdleTimeMillis}")
    private int maxEvictableIdleTimeMillis;

    /** 连接有效性检测 SQL —— 如 "SELECT 1"，用于判断连接是否仍然可用 */
    @Value("${spring.datasource.druid.validationQuery}")
    private String validationQuery;

    /** 是否在空闲时检测连接有效性 —— 推荐开启，不影响性能（后台线程执行） */
    @Value("${spring.datasource.druid.testWhileIdle}")
    private boolean testWhileIdle;

    /** 是否在借出连接时检测有效性 —— 会降低性能，不推荐开启（testWhileIdle 即可） */
    @Value("${spring.datasource.druid.testOnBorrow}")
    private boolean testOnBorrow;

    /** 是否在归还连接时检测有效性 —— 会降低性能，不推荐开启 */
    @Value("${spring.datasource.druid.testOnReturn}")
    private boolean testOnReturn;

    /**
     * 将连接池通用属性应用到目标 DruidDataSource。
     *
     * <p>用于 master/slave 数据源 Bean 创建后统一注入参数，
     * 避免在多个 Bean 中重复配置。</p>
     *
     * @param datasource 待配置的 DruidDataSource（会被原地修改）
     * @return 配置后的数据源（与入参为同一个对象，便于链式调用）
     */
    public DruidDataSource dataSource(DruidDataSource datasource)
    {
        /** 配置初始化大小、最小、最大 */
        datasource.setInitialSize(initialSize);
        datasource.setMaxActive(maxActive);
        datasource.setMinIdle(minIdle);

        /** 配置获取连接等待超时的时间 */
        datasource.setMaxWait(maxWait);
        
        /** 配置驱动连接超时时间，检测数据库建立连接的超时时间，单位是毫秒 */
        datasource.setConnectTimeout(connectTimeout);
        
        /** 配置网络超时时间，等待数据库操作完成的网络超时时间，单位是毫秒 */
        datasource.setSocketTimeout(socketTimeout);

        /** 配置间隔多久才进行一次检测，检测需要关闭的空闲连接，单位是毫秒 */
        datasource.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);

        /** 配置一个连接在池中最小、最大生存的时间，单位是毫秒 */
        datasource.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
        datasource.setMaxEvictableIdleTimeMillis(maxEvictableIdleTimeMillis);

        /**
         * 用来检测连接是否有效的sql，要求是一个查询语句，常用select 'x'。如果validationQuery为null，testOnBorrow、testOnReturn、testWhileIdle都不会起作用。
         */
        datasource.setValidationQuery(validationQuery);
        /** 建议配置为true，不影响性能，并且保证安全性。申请连接的时候检测，如果空闲时间大于timeBetweenEvictionRunsMillis，执行validationQuery检测连接是否有效。 */
        datasource.setTestWhileIdle(testWhileIdle);
        /** 申请连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。 */
        datasource.setTestOnBorrow(testOnBorrow);
        /** 归还连接时执行validationQuery检测连接是否有效，做了这个配置会降低性能。 */
        datasource.setTestOnReturn(testOnReturn);
        return datasource;
    }
}
