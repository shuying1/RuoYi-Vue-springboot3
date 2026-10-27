package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 匿名访问不鉴权注解
 * 
 * 标记在Controller类或方法上，表示该接口无需进行登录鉴权即可匿名访问。
 * 被Spring Security或自定义鉴权过滤器读取，跳过对该注解标记接口的Token校验。
 *
 * 可标记在类级别（整个Controller下所有接口免鉴权）或方法级别（仅当前方法免鉴权）。
 * 适用于登录接口、注册接口、公开API等场景。
 * 
 * @author ruoyi
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Anonymous
{
}
