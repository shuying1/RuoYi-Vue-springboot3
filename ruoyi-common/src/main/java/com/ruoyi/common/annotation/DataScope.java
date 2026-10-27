package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据权限过滤注解
 * 
 * 标记在需要进行数据权限过滤的Mapper方法上，指定SQL中关联的用户表和部门表别名，
 * 以及自定义权限标识。AOP切面会在执行SQL前根据当前登录用户的数据权限范围
 * 动态拼接WHERE条件，实现行级数据安全控制。
 * 
 * @author ruoyi
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope
{
    /**
     * SQL联表查询中用户表的别名，用于权限拼接时定位用户ID字段
     */
    public String userAlias() default "";

    /**
     * SQL联表查询中部门表的别名，用于权限拼接时定位部门ID字段
     */
    public String deptAlias() default "";

    /**
     * 用户表ID字段名，默认为 "user_id"
     */
    public String userField() default "user_id";

    /**
     * 部门表ID字段名，默认为 "dept_id"
     */
    public String deptField() default "dept_id";

    /**
     * 权限字符，用于多个角色匹配符合要求的权限，多个权限用逗号分隔
     * 默认根据权限注解@Ss获取当前用户权限标识进行匹配
     */
    public String permission() default "";
}
