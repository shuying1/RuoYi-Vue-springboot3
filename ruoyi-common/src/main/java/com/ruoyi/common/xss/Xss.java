package com.ruoyi.common.xss;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean Validation XSS 校验注解
 *
 * <p>标记在 Controller 方法参数、DTO 字段或方法返回值上，
 * 使用自定义校验器 {@link XssValidator} 检测字符串中是否包含 HTML 标签
 * （正则 {@code <(\\S*?)[^>]*>.*?|<.*? />}），作为 XssFilter 的补充防线。
 *
 * <p>由于 Filter 层无法校验 Bean Validation，该注解在 Controller 入参校验后触发，
 * 形成 XSS 防护的第二道屏障。
 *
 * <p>技术栈：Jakarta Validation
 *
 * @author ruoyi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.PARAMETER })
@Constraint(validatedBy = { XssValidator.class })
public @interface Xss
{
    String message()

    default "不允许任何脚本运行";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
