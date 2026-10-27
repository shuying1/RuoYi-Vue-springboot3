package com.ruoyi.common.utils;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import com.ruoyi.common.utils.spring.SpringUtils;

/**
 * 国际化消息获取工具类
 *
 * <p>委托 Spring 的 {@link org.springframework.context.MessageSource} 从
 * message.properties（或 message_zh_CN.properties 等）资源文件中
 * 根据错误码和参数数组解析出本地化消息。
 *
 * <p>被 {@link com.ruoyi.common.exception.base.BaseException#getMessage()} 内部调用，
 * 是异常消息国际化的底层支撑。
 *
 * <p>技术栈：Spring Context MessageSource
 *
 * @author ruoyi
 */
public class MessageUtils
{
    /**
     * 根据消息键和参数 获取消息 委托给spring messageSource
     *
     * @param code 消息键
     * @param args 参数
     * @return 获取国际化翻译值
     */
    public static String message(String code, Object... args)
    {
        MessageSource messageSource = SpringUtils.getBean(MessageSource.class);
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
