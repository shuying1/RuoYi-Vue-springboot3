package com.ruoyi.common.core.domain.model;

import lombok.Data;

/**
 * 用户登录请求体 DTO
 *
 * <p>登录接口（POST /login）接收的参数对象，包含用户名、密码、图形验证码和其关联的 UUID。
 * UUID 用于从 Redis 中查找对应的验证码缓存（键为 {@link com.ruoyi.common.constant.CacheConstants#CAPTCHA_CODE_KEY} + UUID）。
 *
 * <p>继承此类的 {@link RegisterBody} 扩展了注册场景（需要额外确认密码等字段）。
 * 使用 @Data 自动生成 getter/setter，前端提交 JSON 时自动反序列化。
 *
 * <p>技术栈：Spring Boot、Lombok @Data
 *
 * @author ruoyi
 */
@Data
public class LoginBody
{
    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码
     */
    private String password;

    /**
     * 验证码
     */
    private String code;

    /**
     * 唯一标识
     */
    private String uuid;


}
