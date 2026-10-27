package com.ruoyi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import com.fasterxml.jackson.annotation.JacksonAnnotationsInside;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.ruoyi.common.config.serializer.SensitiveJsonSerializer;
import com.ruoyi.common.enums.DesensitizedType;

/**
 * 数据脱敏注解
 * 
 * 标记在实体类敏感字段上，使用Jackson序列化时自动对字段值进行脱敏处理。
 * 通过 {@link DesensitizedType} 指定脱敏规则（如手机号中间4位替换为*等）。
 *
 * 采用组合注解方式（@JacksonAnnotationsInside），将自定义序列化器
 * {@link SensitiveJsonSerializer} 嵌入Jackson注解体系。
 * 
 * @author ruoyi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive
{
    /**
     * 脱敏类型，指定字段的脱敏规则（如USERNAME、PHONE、ID_CARD等）
     */
    DesensitizedType desensitizedType();
}
