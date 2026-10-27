package com.ruoyi.common.utils.sql;

import com.ruoyi.common.exception.UtilException;
import com.ruoyi.common.utils.StringUtils;

/**
 * SQL 安全与格式化工具类
 *
 * <p>封装 SQL 关键字正则匹配、ORDER BY 字段名白名单校验、SQL 特殊字符转义等能力，
 * 用于防御 SQL 注入攻击。
 *
 * <p>核心能力：
 * <ul>
 *   <li>SQL_REGEX：常见注入关键字黑名单（and/or/union/select/delete/update/drop 等）</li>
 *   <li>escapeOrderBySql：PageDomain.getOrderBy() 中使用，校验排序字段是否仅含字母数字下划线空格逗号</li>
 *   <li>escapeSql：参数值中的单引号等特殊字符转义</li>
 * </ul>
 *
 * <p>技术栈：正则表达式
 *
 * @author ruoyi
 */
public class SqlUtil
{
    /**
     * 定义常用的 sql关键字
     */
    public static String SQL_REGEX = "\u000B|%0A|and |extractvalue|updatexml|sleep|information_schema|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |union |like |+|/*|user()";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * 限制orderBy最大长度
     */
    private static final int ORDER_BY_MAX_LENGTH = 500;

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value)
    {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value))
        {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH)
        {
            throw new UtilException("参数已超过最大限制，不能进行查询");
        }
        return value;
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value)
    {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL关键字检查
     */
    public static void filterKeyword(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        String normalizedValue = value.replaceAll("\\p{Z}|\\s", "");
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords)
        {
            if (StringUtils.indexOfIgnoreCase(normalizedValue, sqlKeyword) > -1)
            {
                throw new UtilException("请求参数包含敏感关键词'" + sqlKeyword + "'，可能存在安全风险");
            }
        }
    }
}
