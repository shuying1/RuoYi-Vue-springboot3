package com.ruoyi.web.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.ruoyi.common.config.RuoYiConfig;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * OpenAPI / Swagger 接口文档配置
 *
 * <p>通过 Spring Bean 方式向 SpringDoc（OpenAPI 3.0 标准实现）注入自定义 {@link OpenAPI} 对象，
 * 统一管理：</p>
 * <ul>
 *   <li>接口文档元信息（标题、描述、作者、版本号）</li>
 *   <li>全局鉴权方案：声明名为 {@code apikey} 的 Header 安全方案，值为 {@code Authorization: Bearer xxx}，
 *       所有 Swagger 注解了需要鉴权的接口将自动在文档中展示"Authorize"按钮</li>
 * </ul>
 *
 * <p>依赖：{@link RuoYiConfig} 提供系统名称与版本号，用于填充文档标题与版本字段。</p>
 *
 * @author ruoyi
 */
@Configuration
public class SwaggerConfig
{
    /** 系统基础配置，读取项目名称与版本号 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    /**
     * 构造 OpenAPI 对象（SpringDoc 全局入口）
     *
     * <p>配置了 Components（全局安全方案）+ 全局安全需求 + 文档元信息 {@link #getApiInfo()}。</p>
     *
     * @param 无
     * @return OpenAPI SpringDoc 使用的全局 API 描述对象
     */
    @Bean
    public OpenAPI customOpenApi()
    {
        return new OpenAPI().components(new Components()
            // 设置认证的请求头
            .addSecuritySchemes("apikey", securityScheme()))
            .addSecurityItem(new SecurityRequirement().addList("apikey"))
            .info(getApiInfo());
    }

    /**
     * 声明 OpenAPI 安全方案
     *
     * <p>类型：APIKEY；位置：HTTP Header；名称：Authorization；前缀：Bearer。
     * 与系统实际鉴权方式（JWT 放在 Authorization: Bearer token）保持一致。</p>
     *
     * @param 无
     * @return SecurityScheme 描述"Authorization: Bearer xxx"的 Header 安全方案
     */
    @Bean
    public SecurityScheme securityScheme()
    {
        return new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .name("Authorization")
            .in(SecurityScheme.In.HEADER)
            .scheme("Bearer");
    }

    /**
     * 构造文档元信息
     *
     * <p>标题与描述为固定值，作者与版本号动态取自 {@link RuoYiConfig}，
     * 部署时修改配置文件即可同步更新 Swagger 文档。</p>
     *
     * @param 无
     * @return Info OpenAPI 文档的标题、描述、联系人与版本号
     */
    public Info getApiInfo()
    {
        return new Info()
            // 设置标题
            .title("标题：若依管理系统_接口文档")
            // 描述
            .description("描述：用于管理集团旗下公司的人员信息,具体包括XXX,XXX模块...")
            // 作者信息
            .contact(new Contact().name(ruoyiConfig.getName()))
            // 版本
            .version("版本号:" + ruoyiConfig.getVersion());
    }
}
