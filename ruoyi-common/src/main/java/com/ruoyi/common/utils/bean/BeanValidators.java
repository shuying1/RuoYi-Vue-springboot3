package com.ruoyi.common.utils.bean;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;

/**
 * Bean Validation 校验工具类
 *
 * <p>封装 Jakarta Validation（JSR-380）的 {@link Validator} 调用，
 * 提供在非 Spring MVC 环境中手动触发 Bean 属性校验的能力。
 * 校验失败时将 {@link ConstraintViolationException} 中的约束违反消息拼接为可读文本返回。
 *
 * <p>主要方法：
 * <ul>
 *   <li>validateWithException：校验失败时直接抛出 ConstraintViolationException</li>
 *   <li>validateProperty：校验指定属性</li>
 *   <li>validateBean：校验整个 Bean</li>
 *   <li>getValidateMessage：将校验结果集转为字符串消息</li>
 * </ul>
 *
 * <p>技术栈：Jakarta Validation
 *
 * @author ruoyi
 */
public class BeanValidators
{
    public static void validateWithException(Validator validator, Object object, Class<?>... groups)
            throws ConstraintViolationException
    {
        Set<ConstraintViolation<Object>> constraintViolations = validator.validate(object, groups);
        if (!constraintViolations.isEmpty())
        {
            throw new ConstraintViolationException(constraintViolations);
        }
    }
}
