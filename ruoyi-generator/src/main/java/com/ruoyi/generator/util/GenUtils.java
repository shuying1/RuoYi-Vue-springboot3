package com.ruoyi.generator.util;

import java.util.Arrays;
import org.apache.commons.lang3.RegExUtils;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.generator.config.GenConfig;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;

/**
 * 代码生成器命名转换与列属性推断工具类
 *
 * <p>在项目中的角色：在"导入数据库表 → 生成 GenTable/GenTableColumn"这一环节中，
 * 负责自动推导类名、模块名、业务名、Java 类型、默认 HTML 控件类型等，
 * 让用户在"代码生成"界面只需要调整少数选项即可。</p>
 *
 * <p>关键策略：</p>
 * <ul>
 *   <li><b>表名→类名转换</b>：{@link #convertClassName} 先根据配置的表前缀
 *       （如 {@code sys_}）批量剥离，再调用
 *       {@link com.ruoyi.common.utils.StringUtils#convertToCamelCase} 转为 PascalCase；</li>
 *   <li><b>业务名提取</b>：{@link #getBusinessName} 取表名最后一个下划线后的部分；</li>
 *   <li><b>列类型推断</b>：{@link #initColumnField} 是本类最复杂的方法，
 *       对每一列判断：长度→是否文本域（TEXT 或 varchar≥500）、
 *       日期类型→Date + DateTimePicker、
 *       浮点精度→BigDecimal、
 *       整数→Integer / Long；
 *       然后根据列名后缀（status/type/sex/image/file/content/name）
 *       推断默认 HTML 控件类型；</li>
 *   <li><b>默认勾选</b>：默认所有字段都允许 insert/list，
 *       但 BaseEntity 的公共字段（createBy/createTime/remark 等）和主键会被排除。</li>
 * </ul>
 *
 * @author ruoyi
 */
public class GenUtils
{
    /**
     * 初始化代码生成主表信息——推导 className/moduleName/businessName/functionName。
     *
     * <p>这些值来自配置（GenConfig.author / GenConfig.packageName）
     * 和表名转换（tableName → PascalCase）。</p>
     *
     * @param genTable 新建的 GenTable 实体（此时只有 tableName 和 tableComment 来自 DB）
     * @param operName 操作人用户名
     */
    public static void initTable(GenTable genTable, String operName)
    {
        genTable.setClassName(convertClassName(genTable.getTableName()));
        genTable.setPackageName(GenConfig.getPackageName());
        genTable.setModuleName(getModuleName(GenConfig.getPackageName()));
        genTable.setBusinessName(getBusinessName(genTable.getTableName()));
        genTable.setFunctionName(replaceText(genTable.getTableComment()));
        genTable.setFunctionAuthor(GenConfig.getAuthor());
        genTable.setCreateBy(operName);
    }

    /**
     * 初始化列属性——推断 Java 类型、默认控件、默认 insert/list/query 等配置。
     *
     * <p>完整推断链路：</p>
     * <ol>
     *   <li>{@code javaField} = camelCase(columnName)；</li>
     *   <li>根据 {@code columnType} 分类：字符串 / 日期时间 / 数值；</li>
     *   <li>字符串类型：TEXT 或长度 ≥ 500 → textarea，否则 input；</li>
     *   <li>日期时间 → java.util.Date + datetime 控件；</li>
     *   <li>数值：精度 > 0 → BigDecimal，精度 ≤ 10 → Integer，否则 Long；</li>
     *   <li>控件类型再根据列名后缀覆盖（status→radio, type/sex→select,
     *       image→图片上传, file→文件上传, content→富文本）；</li>
     *   <li>默认：insert=true，非主键非 BaseEntity 列 edit=true/list=true，
     *       且以 "name" 结尾的列 queryType 改为 LIKE。</li>
     * </ol>
     *
     * @param column 从数据库元数据拿到的原始列信息
     * @param table  所属主表
     */
    public static void initColumnField(GenTableColumn column, GenTable table)
    {
        String dataType = getDbType(column.getColumnType());
        String columnName = column.getColumnName();
        column.setTableId(table.getTableId());
        column.setCreateBy(table.getCreateBy());
        // 设置java字段名
        column.setJavaField(StringUtils.toCamelCase(columnName));
        // 设置默认类型
        column.setJavaType(GenConstants.TYPE_STRING);
        column.setQueryType(GenConstants.QUERY_EQ);

        if (arraysContains(GenConstants.COLUMNTYPE_STR, dataType) || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType))
        {
            // 字符串长度超过500设置为文本域
            Integer columnLength = getColumnLength(column.getColumnType());
            String htmlType = columnLength >= 500 || arraysContains(GenConstants.COLUMNTYPE_TEXT, dataType) ? GenConstants.HTML_TEXTAREA : GenConstants.HTML_INPUT;
            column.setHtmlType(htmlType);
        }
        else if (arraysContains(GenConstants.COLUMNTYPE_TIME, dataType))
        {
            column.setJavaType(GenConstants.TYPE_DATE);
            column.setHtmlType(GenConstants.HTML_DATETIME);
        }
        else if (arraysContains(GenConstants.COLUMNTYPE_NUMBER, dataType))
        {
            column.setHtmlType(GenConstants.HTML_INPUT);

            // 如果是浮点型 统一用BigDecimal
            String[] str = StringUtils.split(StringUtils.substringBetween(column.getColumnType(), "(", ")"), ",");
            if (str != null && str.length == 2 && Integer.parseInt(str[1]) > 0)
            {
                column.setJavaType(GenConstants.TYPE_BIGDECIMAL);
            }
            // 如果是整形
            else if (str != null && str.length == 1 && Integer.parseInt(str[0]) <= 10)
            {
                column.setJavaType(GenConstants.TYPE_INTEGER);
            }
            // 长整形
            else
            {
                column.setJavaType(GenConstants.TYPE_LONG);
            }
        }

        // 插入字段（默认所有字段都需要插入）
        column.setIsInsert(GenConstants.REQUIRE);

        // 编辑字段
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_EDIT, columnName) && !column.isPk())
        {
            column.setIsEdit(GenConstants.REQUIRE);
        }
        // 列表字段
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_LIST, columnName) && !column.isPk())
        {
            column.setIsList(GenConstants.REQUIRE);
        }
        // 查询字段
        if (!arraysContains(GenConstants.COLUMNNAME_NOT_QUERY, columnName) && !column.isPk())
        {
            column.setIsQuery(GenConstants.REQUIRE);
        }

        // 查询字段类型
        if (StringUtils.endsWithIgnoreCase(columnName, "name"))
        {
            column.setQueryType(GenConstants.QUERY_LIKE);
        }
        // 状态字段设置单选框
        if (StringUtils.endsWithIgnoreCase(columnName, "status"))
        {
            column.setHtmlType(GenConstants.HTML_RADIO);
        }
        // 类型&性别字段设置下拉框
        else if (StringUtils.endsWithIgnoreCase(columnName, "type")
                || StringUtils.endsWithIgnoreCase(columnName, "sex"))
        {
            column.setHtmlType(GenConstants.HTML_SELECT);
        }
        // 图片字段设置图片上传控件
        else if (StringUtils.endsWithIgnoreCase(columnName, "image"))
        {
            column.setHtmlType(GenConstants.HTML_IMAGE_UPLOAD);
        }
        // 文件字段设置文件上传控件
        else if (StringUtils.endsWithIgnoreCase(columnName, "file"))
        {
            column.setHtmlType(GenConstants.HTML_FILE_UPLOAD);
        }
        // 内容字段设置富文本控件
        else if (StringUtils.endsWithIgnoreCase(columnName, "content"))
        {
            column.setHtmlType(GenConstants.HTML_EDITOR);
        }
    }

    /**
     * 判断数组中是否包含指定值。
     *
     * @param arr 目标数组
     * @param targetValue 待查找值
     * @return true 表示存在
     */
    public static boolean arraysContains(String[] arr, String targetValue)
    {
        return Arrays.asList(arr).contains(targetValue);
    }

    /**
     * 从完整包名提取最后一段作为模块名。
     *
     * <p>示例：{@code com.ruoyi.system} → {@code system}</p>
     *
     * @param packageName 完整包名
     * @return 模块名（最后一段）
     */
    public static String getModuleName(String packageName)
    {
        int lastIndex = packageName.lastIndexOf(".");
        int nameLength = packageName.length();
        return StringUtils.substring(packageName, lastIndex + 1, nameLength);
    }

    /**
     * 从表名提取最后一段下划线后的内容作为业务名。
     *
     * <p>示例：{@code sys_user} → {@code user}；{@code biz_order_item} → {@code item}。</p>
     *
     * @param tableName 原始表名
     * @return 业务名
     */
    public static String getBusinessName(String tableName)
    {
        int lastIndex = tableName.lastIndexOf("_");
        int nameLength = tableName.length();
        return StringUtils.substring(tableName, lastIndex + 1, nameLength);
    }

    /**
     * 将数据库表名转换为 Java 类名（PascalCase）。
     *
     * <p>转换流程：</p>
     * <ol>
     *   <li>若配置了 autoRemovePre=true 且 tablePrefix 非空，
     *       从前往后检查表名是否以任一前缀开头，命中则剥掉；</li>
     *   <li>对剩余部分调 {@code convertToCamelCase}（内置下划线→驼峰 + 首字母大写）。</li>
     * </ol>
     * <p>示例：{@code sys_user} + 前缀 {@code sys_} → {@code User}；
     * {@code t_order_item} + 前缀 {@code t_} → {@code OrderItem}。</p>
     *
     * @param tableName 原始数据库表名
     * @return 转换后的 PascalCase 类名
     */
    public static String convertClassName(String tableName)
    {
        boolean autoRemovePre = GenConfig.getAutoRemovePre();
        String tablePrefix = GenConfig.getTablePrefix();
        if (autoRemovePre && StringUtils.isNotEmpty(tablePrefix))
        {
            String[] searchList = StringUtils.split(tablePrefix, ",");
            tableName = replaceFirst(tableName, searchList);
        }
        return StringUtils.convertToCamelCase(tableName);
    }

    /**
     * 批量尝试替换表名前缀——命中第一个就停止。
     *
     * @param replacementm 原始字符串（表名）
     * @param searchList  可能的前缀列表
     * @return 替换后的字符串；若未命中任何前缀则原样返回
     */
    public static String replaceFirst(String replacementm, String[] searchList)
    {
        String text = replacementm;
        for (String searchString : searchList)
        {
            if (replacementm.startsWith(searchString))
            {
                text = replacementm.replaceFirst(searchString, "");
                break;
            }
        }
        return text;
    }

    /**
     * 去掉表注释中的"表"或"若依"字样，用作功能名/类注释。
     *
     * @param text 原始表注释（如 "用户信息表"）
     * @return 去掉敏感词后的结果（如 "用户信息"）
     */
    public static String replaceText(String text)
    {
        return RegExUtils.replaceAll(text, "(?:表|若依)", "");
    }

    /**
     * 截取数据库列类型的基础类型名（去掉长度参数）。
     *
     * <p>示例：{@code varchar(64)} → {@code varchar}；{@code datetime} → {@code datetime}。</p>
     *
     * @param columnType 原始列类型字符串
     * @return 括号前的基础类型
     */
    public static String getDbType(String columnType)
    {
        if (StringUtils.indexOf(columnType, "(") > 0)
        {
            return StringUtils.substringBefore(columnType, "(");
        }
        else
        {
            return columnType;
        }
    }

    /**
     * 从带括号的列类型中提取长度/精度数字。
     *
     * <p>示例：{@code varchar(200)} → 200；{@code bigint} → 0。</p>
     *
     * @param columnType 原始列类型字符串
     * @return 括号内的数字；无括号返回 0
     */
    public static Integer getColumnLength(String columnType)
    {
        if (StringUtils.indexOf(columnType, "(") > 0)
        {
            String length = StringUtils.substringBetween(columnType, "(", ")");
            return Integer.valueOf(length);
        }
        else
        {
            return 0;
        }
    }
}
