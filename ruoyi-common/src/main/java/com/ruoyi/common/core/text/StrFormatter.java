package com.ruoyi.common.core.text;

import com.ruoyi.common.utils.StringUtils;

/**
 * 占位符字符串格式化工具类
 * 
 * 实现类似 SLF4J 的 "{}" 占位符替换机制，将模板中的 {} 按顺序替换为传入的参数值。
 * 支持使用反斜杠转义占位符：
 * - 普通使用：format("this is {} for {}", "a", "b") → "this is a for b"
 * - 转义{}：format("this is \\{} for {}", "a", "b") → "this is \{} for a"
 * - 连续反斜杠：format("this is \\\\{} for {}", "a", "b") → "this is \a for b"
 * 
 * @author ruoyi
 */
public class StrFormatter
{
    /** 占位符标记常量 */
    public static final String EMPTY_JSON = "{}";
    /** 反斜杠转义字符 */
    public static final char C_BACKSLASH = '\\';
    /** 占位符左边界 */
    public static final char C_DELIM_START = '{';
    /** 占位符右边界 */
    public static final char C_DELIM_END = '}';

    /**
     * 格式化字符串：将模板中的 "{}" 占位符按顺序替换为参数值
     * 
     * @param strPattern 字符串模板，可为null或空串
     * @param argArray   替换参数数组
     * @return 格式化后的字符串；若模板或参数数组为空则返回原模板
     */
    public static String format(final String strPattern, final Object... argArray)
    {
        if (StringUtils.isEmpty(strPattern) || StringUtils.isEmpty(argArray))
        {
            return strPattern;
        }
        final int strPatternLength = strPattern.length();

        // 预分配StringBuilder容量以提升性能
        StringBuilder sbuf = new StringBuilder(strPatternLength + 50);

        int handledPosition = 0;
        // 当前占位符在模板中的位置
        int delimIndex;
        for (int argIndex = 0; argIndex < argArray.length; argIndex++)
        {
            delimIndex = strPattern.indexOf(EMPTY_JSON, handledPosition);
            if (delimIndex == -1)
            {
                if (handledPosition == 0)
                {
                    // 模板中没有任何占位符，直接返回原模板
                    return strPattern;
                }
                else
                {
                    // 剩余模板中没有占位符，追加剩余部分后返回
                    sbuf.append(strPattern, handledPosition, strPatternLength);
                    return sbuf.toString();
                }
            }
            else
            {
                if (delimIndex > 0 && strPattern.charAt(delimIndex - 1) == C_BACKSLASH)
                {
                    if (delimIndex > 1 && strPattern.charAt(delimIndex - 2) == C_BACKSLASH)
                    {
                        // 占位符前有两个反斜杠，说明转义符本身被转义了，占位符仍然有效
                        sbuf.append(strPattern, handledPosition, delimIndex - 1);
                        sbuf.append(Convert.utf8Str(argArray[argIndex]));
                        handledPosition = delimIndex + 2;
                    }
                    else
                    {
                        // 占位符被单个反斜杠转义，按字面输出 "{}"
                        argIndex--;
                        sbuf.append(strPattern, handledPosition, delimIndex - 1);
                        sbuf.append(C_DELIM_START);
                        handledPosition = delimIndex + 1;
                    }
                }
                else
                {
                    // 正常占位符，追加参数值
                    sbuf.append(strPattern, handledPosition, delimIndex);
                    sbuf.append(Convert.utf8Str(argArray[argIndex]));
                    handledPosition = delimIndex + 2;
                }
            }
        }
        // 追加最后一个占位符后剩余的模板内容
        sbuf.append(strPattern, handledPosition, strPattern.length());

        return sbuf.toString();
    }
}
