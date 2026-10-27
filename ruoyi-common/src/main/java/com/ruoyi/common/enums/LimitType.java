package com.ruoyi.common.enums;

/**
 * 接口限流类型枚举
 * 
 * 用于 @RateLimiter 注解中指定限流策略，支持全局和按IP两种方式。
 * 
 * @author ruoyi
 */

public enum LimitType
{
    /**
     * 默认策略：全局限流，所有用户共享同一个计数器
     */
    DEFAULT,

    /**
     * 按请求者IP限流，每个IP地址独立计数
     */
    IP
}
