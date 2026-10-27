package com.ruoyi.framework.aspectj;

import java.util.Objects;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.DataSource;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.datasource.DynamicDataSourceContextHolder;

/**
 * 动态数据源切换切面
 *
 * <p>设计意图：基于 AOP + 注解方式实现多数据源的自动切换。
 * 通过扫描方法或类上标注的 {@link DataSource} 注解，在方法执行前
 * 将目标数据源名称写入 {@link DynamicDataSourceContextHolder} 的 ThreadLocal 中，
 * 使后续的 MyBatis / JDBC 操作路由到对应的 {@link DynamicDataSource} 中配置的物理数据源；
 * 方法执行完毕后在 finally 中清除上下文，防止数据源泄漏。</p>
 *
 * <p>切面流程（Around 环绕通知）：</p>
 * <ol>
 *   <li>通过 {@link #getDataSource} 查找当前方法 / 类上的 {@link DataSource} 注解（优先方法级，其次类级）</li>
 *   <li>若找到注解，将注解的 value（数据源枚举名）写入 ThreadLocal</li>
 *   <li>执行目标方法 {@code point.proceed()}</li>
 *   <li>finally 中无条件清理 ThreadLocal，避免线程复用时数据源串用</li>
 * </ol>
 *
 * <p>生效顺序：{@link Order} 值为 1，确保在其他业务切面之前完成数据源切换。</p>
 *
 * <p>支持的 {@link DataSource} 枚举值通常包含：master（主库）、slave（从库），
 * 可在 DynamicDataSource 配置中扩展更多数据源。</p>
 *
 * @author ruoyi
 * @see DataSource 数据源切换注解
 * @see DynamicDataSourceContextHolder 线程级数据源上下文持有者
 * @see DynamicDataSource 基于 AbstractRoutingDataSource 的实际数据源路由器
 */
@Aspect
@Order(1)
@Component
public class DataSourceAspect
{
    /** 切面日志记录器，用于数据源切换异常或调试信息输出 */
    protected Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 切点定义：匹配所有标注了 {@link DataSource} 注解的方法或类。
     *
     * <p>表达式同时支持方法级（@annotation）和类级（@within）注解，
     * 即可以在 Controller/Service 类上统一指定，也可以在单个方法上覆盖指定。</p>
     */
    @Pointcut("@annotation(com.ruoyi.common.annotation.DataSource)"
            + "|| @within(com.ruoyi.common.annotation.DataSource)")
    public void dsPointCut()
    {

    }

    /**
     * 环绕通知：数据源切换的核心逻辑。
     *
     * <p>在 {@code point.proceed()} 之前设置 ThreadLocal 数据源标识，
     * 之后（无论成功或异常）通过 finally 清除。</p>
     *
     * @param point 被拦截的连接点
     * @return 目标方法的返回值
     * @throws Throwable 目标方法抛出的任何异常
     */
    @Around("dsPointCut()")
    public Object around(ProceedingJoinPoint point) throws Throwable
    {
        DataSource dataSource = getDataSource(point);

        if (StringUtils.isNotNull(dataSource))
        {
            // 将注解指定的数据源枚举名写入 ThreadLocal，供 DynamicDataSource.determineCurrentLookupKey() 读取
            DynamicDataSourceContextHolder.setDataSourceType(dataSource.value().name());
        }

        try
        {
            return point.proceed();
        }
        finally
        {
            // 销毁数据源上下文，在执行方法之后必须清除，防止线程池复用时数据源串用
            DynamicDataSourceContextHolder.clearDataSourceType();
        }
    }

    /**
     * 获取需要切换的 {@link DataSource} 注解实例。
     *
     * <p>查找优先级：方法级注解 → 类级注解。
     * 如果方法上显式标注了 {@link DataSource}，则优先使用；
     * 否则回退到类上的注解（适用于整个 Service 默认使用 slave 从库，
     * 但某个特定写操作方法上指定 master 主库的场景）。</p>
     *
     * @param point AOP 连接点，包含目标方法签名信息
     * @return 找到的 DataSource 注解，若方法和类都未标注则返回 null
     */
    public DataSource getDataSource(ProceedingJoinPoint point)
    {
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 优先查找方法上的 @DataSource 注解
        DataSource dataSource = AnnotationUtils.findAnnotation(signature.getMethod(), DataSource.class);
        if (Objects.nonNull(dataSource))
        {
            return dataSource;
        }

        // 方法未标注，回退查找类上的 @DataSource 注解
        return AnnotationUtils.findAnnotation(signature.getDeclaringType(), DataSource.class);
    }
}
