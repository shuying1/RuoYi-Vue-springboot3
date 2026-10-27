package com.ruoyi.common.config.serializer;

import java.io.IOException;
import java.util.Objects;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import com.ruoyi.common.annotation.Sensitive;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.DesensitizedType;
import com.ruoyi.common.utils.SecurityUtils;

/**
 * 数据脱敏自定义 JSON 序列化器
 *
 * <p>实现 Jackson 的 {@link JsonSerializer} 和 {@link ContextualSerializer} 接口，
 * 序列化时根据 {@link Sensitive} 注解指定的 {@link DesensitizedType} 对字符串进行脱敏处理。
 * 仅在当前登录用户不是超级管理员时生效（管理员可查看完整敏感数据），
 * 无登录上下文时默认脱敏。
 *
 * <p>采用 ContextualSerializer 机制，在创建序列化上下文时从 BeanProperty 中
 * 读取 @Sensitive 注解并缓存 desensitizedType，避免每次序列化都反射读取注解。
 *
 * <p>技术栈：Jackson
 *
 * @author ruoyi
 */
public class SensitiveJsonSerializer extends JsonSerializer<String> implements ContextualSerializer
{
    private DesensitizedType desensitizedType;

    /**
     * 执行序列化：根据当前登录用户是否为管理员决定是否脱敏后写入 JSON
     *
     * @param value       原始待脱敏的字符串值
     * @param gen         JSON 生成器
     * @param serializers 序列化器提供者
     * @throws IOException 写入异常
     */
    @Override
    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (desensitization())
        {
            gen.writeString(desensitizedType.desensitizer().apply(value));
        }
        else
        {
            gen.writeString(value);
        }
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
            throws JsonMappingException
    {
        Sensitive annotation = property.getAnnotation(Sensitive.class);
        if (Objects.nonNull(annotation) && Objects.equals(String.class, property.getType().getRawClass()))
        {
            this.desensitizedType = annotation.desensitizedType();
            return this;
        }
        return prov.findValueSerializer(property.getType(), property);
    }

    /**
     * 是否需要脱敏处理
     */
    private boolean desensitization()
    {
        try
        {
            LoginUser securityUser = SecurityUtils.getLoginUser();
            // 管理员不脱敏
            return !securityUser.getUser().isAdmin();
        }
        catch (Exception e)
        {
            return true;
        }
    }
}
