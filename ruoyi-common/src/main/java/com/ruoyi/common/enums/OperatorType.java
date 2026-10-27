package com.ruoyi.common.enums;

/**
 * 操作人类别枚举
 * 
 * 在 @Log 操作日志注解中使用，标识执行该操作的用户类型。
 * 
 * @author ruoyi
 */
public enum OperatorType
{
    /**
     * 其他类型（默认）
     */
    OTHER,

    /**
     * 后台管理用户（浏览器端操作）
     */
    MANAGE,

    /**
     * 手机端用户（移动端APP操作）
     */
    MOBILE
}
