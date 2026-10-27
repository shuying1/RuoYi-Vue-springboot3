package com.ruoyi.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义注解防止表单重复提交
 * 
 * 标记在需要防重提交的Controller方法上，基于Redis实现请求去重。
 * 在指定时间间隔内（默认5秒）相同的请求参数视为重复提交，直接返回错误提示。
 * 适用于创建订单、支付、保存表单等场景。
 *
 * 标注了 @Inherited，子类会继承父类的防重复提交注解。
 * 
 * @author ruoyi
 *
 */
@Inherited
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RepeatSubmit
{
    /**
     * 防重提交时间间隔，单位毫秒，小于此时间视为重复提交，默认5000ms
     */
    public int interval() default 5000;

    /**
     * 重复提交时返回的提示消息
     */
    public String message() default "不允许重复提交，请稍候再试";
}
