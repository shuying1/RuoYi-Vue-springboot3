package com.ruoyi.common.core.text;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import com.ruoyi.common.utils.StringUtils;

/**
 * 字符集编码转换工具类
 *
 * <p>提供字符串在不同字符集间的互相转换，支持：
 * <ul>
 *   <li>{@link #charset(String)}：字符串转 {@link Charset} 对象（空时回退到 JVM 默认字符集）</li>
 *   <li>{@link #convert(String, String, String)}：两字符集间的字符串重编码，
 *       默认源字符集 ISO-8859-1（常用于修正 tomcat GET 请求中文乱码），目标字符集 UTF-8</li>
 *   <li>{@link #systemCharset()}：获取当前 JVM 默认字符集名称</li>
 * </ul>
 *
 * <p>技术栈：JDK NIO Charset
 *
 * @author ruoyi
 */
public class CharsetKit
{
    /** ISO-8859-1 */
    public static final String ISO_8859_1 = "ISO-8859-1";
    /** UTF-8 */
    public static final String UTF_8 = "UTF-8";
    /** GBK */
    public static final String GBK = "GBK";

    /** ISO-8859-1 */
    public static final Charset CHARSET_ISO_8859_1 = Charset.forName(ISO_8859_1);
    /** UTF-8 */
    public static final Charset CHARSET_UTF_8 = Charset.forName(UTF_8);
    /** GBK */
    public static final Charset CHARSET_GBK = Charset.forName(GBK);

    /**
     * 转换为Charset对象
     * 
     * @param charset 字符集，为空则返回默认字符集
     * @return Charset
     */
    public static Charset charset(String charset)
    {
        return StringUtils.isEmpty(charset) ? Charset.defaultCharset() : Charset.forName(charset);
    }

    /**
     * 转换字符串的字符集编码
     * 
     * @param source 字符串
     * @param srcCharset 源字符集，默认ISO-8859-1
     * @param destCharset 目标字符集，默认UTF-8
     * @return 转换后的字符集
     */
    public static String convert(String source, String srcCharset, String destCharset)
    {
        return convert(source, Charset.forName(srcCharset), Charset.forName(destCharset));
    }

    /**
     * 转换字符串的字符集编码
     * 
     * @param source 字符串
     * @param srcCharset 源字符集，默认ISO-8859-1
     * @param destCharset 目标字符集，默认UTF-8
     * @return 转换后的字符集
     */
    public static String convert(String source, Charset srcCharset, Charset destCharset)
    {
        if (null == srcCharset)
        {
            srcCharset = StandardCharsets.ISO_8859_1;
        }

        if (null == destCharset)
        {
            destCharset = StandardCharsets.UTF_8;
        }

        if (StringUtils.isEmpty(source) || srcCharset.equals(destCharset))
        {
            return source;
        }
        return new String(source.getBytes(srcCharset), destCharset);
    }

    /**
     * @return 系统字符集编码
     */
    public static String systemCharset()
    {
        return Charset.defaultCharset().name();
    }
}
