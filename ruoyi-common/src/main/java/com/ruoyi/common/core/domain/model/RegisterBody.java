package com.ruoyi.common.core.domain.model;

/**
 * 用户注册请求体 DTO
 *
 * <p>继承 {@link LoginBody}，在其基础上扩展注册业务所需字段（如确认密码、昵称、邮箱、手机号等）。
 * 子类在 ruoyi-framework 模块的 RegisterController 中使用。
 *
 * <p>技术栈：Spring Boot、Lombok @Data
 *
 * @author ruoyi
 */
public class RegisterBody extends LoginBody
{

}
