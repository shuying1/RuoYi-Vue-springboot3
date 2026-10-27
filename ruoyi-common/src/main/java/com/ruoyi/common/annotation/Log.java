package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.OperatorType;

/**
 * 自定义操作日志记录注解
 * 
 * 标记在Controller方法或参数上，AOP切面会自动拦截并记录操作日志到数据库。
 * 可配置日志标题（模块名）、业务操作类型、操作人类别、是否保存请求/响应参数、
 * 以及需要排除的敏感请求参数名。
 *
 * 典型用法：@Log(title = "用户管理", businessType = BusinessType.INSERT)
 * 
 * @author ruoyi
 *
 */
@Target({ ElementType.PARAMETER, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log
{
    /**
     * 日志标题，通常对应功能模块名称（如 "用户管理"）
     */
    public String title() default "";

    /**
     * 业务操作类型，默认其他 {@link BusinessType#OTHER}
     */
    public BusinessType businessType() default BusinessType.OTHER;

    /**
     * 操作人类别，默认后台管理用户 {@link OperatorType#MANAGE}
     */
    public OperatorType operatorType() default OperatorType.MANAGE;

    /**
     * 是否保存请求参数，默认true
     */
    public boolean isSaveRequestData() default true;

    /**
     * 是否保存响应参数，默认true
     */
    public boolean isSaveResponseData() default true;

    /**
     * 排除指定的请求参数不记录日志（如密码、token等敏感字段）
     */
    public String[] excludeParamNames() default {};
}
