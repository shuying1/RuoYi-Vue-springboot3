package com.ruoyi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Excel注解集
 * 
 * 当实体类需要同时导出多个Excel Sheet（如主子表场景）时使用此注解。
 * 可在一个字段上标记多个 {@link Excel} 注解，实现多Sheet导出。
 * 
 * @author ruoyi
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Excels
{
    /**
     * 多个Excel注解的数组
     */
    public Excel[] value();
}
