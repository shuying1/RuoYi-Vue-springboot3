package com.ruoyi.framework.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceBuilder;
import com.alibaba.druid.spring.boot3.autoconfigure.properties.DruidStatProperties;
import com.alibaba.druid.util.Utils;
import com.ruoyi.common.enums.DataSourceType;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.framework.config.properties.DruidProperties;
import com.ruoyi.framework.datasource.DynamicDataSource;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

/**
 * Druid 多数据源配置
 *
 * <p>设计意图：基于阿里巴巴 Druid 连接池构建主从多数据源架构。</p>
 *
 * <p>数据源定义：</p>
 * <ul>
 *   <li><b>masterDataSource</b> —— 主库，必选，从 {@code spring.datasource.druid.master.*} 读取配置</li>
 *   <li><b>slaveDataSource</b> —— 从库，可选，仅当 {@code spring.datasource.druid.slave.enabled=true} 时创建
 *   <li><b>dynamicDataSource</b> —— 动态路由数据源（{@link DynamicDataSource}），标注 {@link Primary}，
 *       将 master + slave 组合起来，供 {@link DataSourceAspect} 按注解切换</li>
 * </ul>
 *
 * <p>额外功能：当 {@code spring.datasource.druid.statViewServlet.enabled=true} 时，
 * 注册一个 Filter 对 Druid 监控页面的 common.js 进行响应改写，去除底部广告信息。</p>
 *
 * @author ruoyi
 * @see DynamicDataSource 动态数据源路由器
 * @see DataSourceAspect 数据源切换 AOP 切面
 * @see DruidProperties Druid 连接池通用属性配置
 */
@Configuration
public class DruidConfig
{
    /**
     * 主库数据源 Bean。
     *
     * <p>配置前缀 {@code spring.datasource.druid.master.*}（url、username、password、driver-class-name 等），
     * 再由 {@link DruidProperties#dataSource} 统一补充连接池参数（initialSize、maxActive、maxWait 等）。</p>
     *
     * @param druidProperties Druid 连接池通用属性 Bean，用于补充连接池参数
     * @return 配置完成的 DruidDataSource（主库）
     */
    @Bean
    @ConfigurationProperties("spring.datasource.druid.master")
    public DataSource masterDataSource(DruidProperties druidProperties)
    {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    /**
     * 从库数据源 Bean（可选）。
     *
     * <p>仅当配置文件中 {@code spring.datasource.druid.slave.enabled=true} 时才创建。
     * 若未启用，{@link #dataSource} 中的 {@link #setDataSource} 会捕获异常并静默跳过。</p>
     *
     * @param druidProperties Druid 连接池通用属性 Bean
     * @return 配置完成的 DruidDataSource（从库）
     */
    @Bean
    @ConfigurationProperties("spring.datasource.druid.slave")
    @ConditionalOnProperty(prefix = "spring.datasource.druid.slave", name = "enabled", havingValue = "true")
    public DataSource slaveDataSource(DruidProperties druidProperties)
    {
        DruidDataSource dataSource = DruidDataSourceBuilder.create().build();
        return druidProperties.dataSource(dataSource);
    }

    /**
     * 动态数据源路由器 Bean（Spring 容器主数据源）。
     *
     * <p>将 masterDataSource 设为默认数据源（未指定注解时使用），
     * 同时将 master + slave 组装进 targetDataSources Map，
     * 交给 {@link DynamicDataSource} 作为路由表。</p>
     *
     * @param masterDataSource 主库 Bean（Spring 自动注入）
     * @return DynamicDataSource 实例，后续由 {@link DataSourceAspect} 根据 @DataSource 注解切换
     */
    @Bean(name = "dynamicDataSource")
    @Primary
    public DynamicDataSource dataSource(DataSource masterDataSource)
    {
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(DataSourceType.MASTER.name(), masterDataSource);
        // 尝试注入 slaveDataSource Bean（若未启用则静默忽略）
        setDataSource(targetDataSources, DataSourceType.SLAVE.name(), "slaveDataSource");
        return new DynamicDataSource(masterDataSource, targetDataSources);
    }
    
    /**
     * 将已注册的 Bean 加入目标数据源映射表中。
     *
     * <p>尝试从 Spring 容器获取指定名称的 Bean，若不存在（未启用从库）则静默忽略异常。
     * 避免因为 ConditionalOnProperty 导致 Bean 缺失而启动失败。</p>
     * 
     * @param targetDataSources 备选数据源集合（会被追加新的数据源）
     * @param sourceName 数据源名称（如 "slave"），作为 Map 的 Key
     * @param beanName Spring 容器中的 Bean 名称（如 "slaveDataSource"）
     */
    public void setDataSource(Map<Object, Object> targetDataSources, String sourceName, String beanName)
    {
        try
        {
            DataSource dataSource = SpringUtils.getBean(beanName);
            targetDataSources.put(sourceName, dataSource);
        }
        catch (Exception e)
        {
            // Bean 不存在时静默忽略（如未启用从库场景），不中断启动
        }
    }

    /**
     * 注册 Filter 去除 Druid 监控页面底部的第三方广告信息。
     *
     * <p>实现原理：拦截 {@code /druid/js/common.js} 请求，
     * 在 Filter 链执行完成后，读取原始 JS 内容并用正则替换掉广告 Banner 和 "powered by" 信息。</p>
     *
     * @param properties Druid 监控配置属性（包含 StatViewServlet 的 urlPattern）
     * @return FilterRegistrationBean，Spring Boot 自动注册为 Servlet Filter
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Bean
    @ConditionalOnProperty(name = "spring.datasource.druid.statViewServlet.enabled", havingValue = "true")
    public FilterRegistrationBean removeDruidFilterRegistrationBean(DruidStatProperties properties)
    {
        // 获取 Druid 监控页面的 urlPattern（默认 /druid/*）
        DruidStatProperties.StatViewServlet config = properties.getStatViewServlet();
        String pattern = config.getUrlPattern() != null ? config.getUrlPattern() : "/druid/*";
        // 推导出 common.js 的实际请求路径（如 /druid/js/common.js）
        String commonJsPattern = pattern.replaceAll("\\*", "js/common.js");
        final String filePath = "support/http/resources/js/common.js";
        // 创建动态 Filter，拦截 common.js 响应并改写内容
        Filter filter = new Filter()
        {
            @Override
            public void init(jakarta.servlet.FilterConfig filterConfig) throws ServletException
            {
            }
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException
            {
                chain.doFilter(request, response);
                // 重置缓冲区（保留响应头），后续写入替换后的内容
                response.resetBuffer();
                // 从 Druid 源码 jar 中读取 common.js 默认内容
                String text = Utils.readFromResource(filePath);
                // 正则替换：去除底部广告 Banner 和 "powered by shrek.wang" 文字
                text = text.replaceAll("<a.*?banner\"></a><br/>", "");
                text = text.replaceAll("powered.*?shrek.wang</a>", "");
                response.getWriter().write(text);
            }
            @Override
            public void destroy()
            {
            }
        };
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        registrationBean.setFilter(filter);
        registrationBean.addUrlPatterns(commonJsPattern);
        return registrationBean;
    }
}
