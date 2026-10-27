package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.ruoyi.common.enums.DataSourceType;

/**
 * 自定义多数据源切换注解
 * 
 * 标记在Service方法或类上，用于在运行时动态切换当前线程使用的数据源。
 * 通过AOP切面拦截注解标记的方法，在方法执行前设置ThreadLocal中的数据源类型，
 * 方法执行后清理ThreadLocal。
 *
 * 优先级：先方法，后类。如果方法上覆盖了类上的数据源类型，以方法的为准。
 * 标注了 @Inherited，子类会继承父类的数据源注解。
 * 
 * @author ruoyi
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DataSource
{
    /**
     * 切换数据源名称，默认使用主库 {@link DataSourceType#MASTER}
     */
    public DataSourceType value() default DataSourceType.MASTER;
}
