package com.ruoyi.common.xss;

import com.ruoyi.common.utils.StringUtils;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * XSS 注解校验器实现
 *
 * <p>实现 {@link ConstraintValidator} 接口，在 Bean Validation 阶段检测字符串中
 * 是否含有 HTML 标签（正则 {@code <(\\S*?)[^>]*>.*?|<.*? />}）。
 * 空字符串视为校验通过。检测到 HTML 则返回 false 触发校验失败，
 * 由 ControllerAdvice 捕获后转换为友好错误提示。
 *
 * <p>技术栈：Jakarta Validation
 *
 * @author ruoyi
 */
public class XssValidator implements ConstraintValidator<Xss, String>
{
    private static final String HTML_PATTERN = "<(\\S*?)[^>]*>.*?|<.*? />";

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext)
    {
        if (StringUtils.isBlank(value))
        {
            return true;
        }
        return !containsHtml(value);
    }

    public static boolean containsHtml(String value)
    {
        StringBuilder sHtml = new StringBuilder();
        Pattern pattern = Pattern.compile(HTML_PATTERN);
        Matcher matcher = pattern.matcher(value);
        while (matcher.find())
        {
            sHtml.append(matcher.group());
        }
        return pattern.matcher(sHtml).matches();
    }
}