package com.ruoyi.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.math.BigDecimal;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import com.ruoyi.common.utils.poi.ExcelHandlerAdapter;

/**
 * 自定义导出Excel数据注解
 * 
 * 标记在实体类字段上，指定该字段在导出Excel时的列名、格式、宽度、对齐方式、
 * 字典转换、类型转换、自定义处理器等丰富属性。
 * 同时支持导入时的校验规则配置。
 * 
 * @author ruoyi
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Excel
{
    /**
     * 导出时在excel中排序，值越小越靠前，默认Integer.MAX_VALUE
     */
    public int sort() default Integer.MAX_VALUE;

    /**
     * 导出到Excel中的列名称
     */
    public String name() default "";

    /**
     * 日期格式化模式，如 "yyyy-MM-dd"
     */
    public String dateFormat() default "";

    /**
     * 如果是字典类型，请设置字典的type值（如: sys_user_sex），导入导出时自动转换字典值
     */
    public String dictType() default "";

    /**
     * 读取内容转表达式（如: 0=男,1=女,2=未知），用于导入时的枚举值映射
     */
    public String readConverterExp() default "";

    /**
     * 分隔符，读取字符串数组内容时使用，默认英文逗号
     */
    public String separator() default ",";

    /**
     * BigDecimal精度，默认-1表示不开启精度格式化
     */
    public int scale() default -1;

    /**
     * BigDecimal舍入规则，默认 {@link BigDecimal#ROUND_HALF_EVEN}（银行家舍入）
     */
    @SuppressWarnings("deprecation")
    public int roundingMode() default BigDecimal.ROUND_HALF_EVEN;

    /**
     * 导出时每个列的高度（Excel行高），默认14
     */
    public double height() default 14;

    /**
     * 导出时每个列的宽度，默认16
     */
    public double width() default 16;

    /**
     * 文字后缀，如设置为 "%" 后 90 变成 90%
     */
    public String suffix() default "";

    /**
     * 当字段值为空时的默认填充值
     */
    public String defaultValue() default "";

    /**
     * 单元格提示信息，Excel批注内容
     */
    public String prompt() default "";

    /**
     * 是否允许内容换行
     */
    public boolean wrapText() default false;

    /**
     * 设置下拉选择框的候选项内容，如 {"选项1", "选项2"}
     */
    public String[] combo() default {};

    /**
     * 是否从字典读取数据填充下拉框，需要配合 {@link #dictType} 使用
     */
    public boolean comboReadDict() default false;

    /**
     * 是否需要纵向合并单元格，适用于含list集合的嵌套字段
     */
    public boolean needMerge() default false;

    /**
     * 是否导出数据，模板导出场景下可设置为false仅导出表头
     */
    public boolean isExport() default true;

    /**
     * 另一个类中的属性名称，支持多级对象取值，以小数点隔开（如 "dept.deptName"）
     */
    public String targetAttr() default "";

    /**
     * 是否自动统计数据，在导出表最后追加一行合计
     */
    public boolean isStatistics() default false;

    /**
     * 导出类型，0-数字 1-字符串 2-图片 3-文本
     */
    public ColumnType cellType() default ColumnType.STRING;

    /**
     * 导出列头背景颜色
     */
    public IndexedColors headerBackgroundColor() default IndexedColors.GREY_50_PERCENT;

    /**
     * 导出列头字体颜色
     */
    public IndexedColors headerColor() default IndexedColors.WHITE;

    /**
     * 导出单元格背景颜色
     */
    public IndexedColors backgroundColor() default IndexedColors.WHITE;

    /**
     * 导出单元格字体颜色
     */
    public IndexedColors color() default IndexedColors.BLACK;

    /**
     * 导出字段对齐方式，默认居中
     */
    public HorizontalAlignment align() default HorizontalAlignment.CENTER;

    /**
     * 自定义数据处理器，用于特殊数据的转换逻辑
     */
    public Class<?> handler() default ExcelHandlerAdapter.class;

    /**
     * 自定义数据处理器参数数组
     */
    public String[] args() default {};

    /**
     * 字段类型，控制导入导出行为：ALL(0)-导入导出，EXPORT(1)-仅导出，IMPORT(2)-仅导入
     */
    Type type() default Type.ALL;

    /**
     * Excel注解的导入导出类型枚举
     */
    public enum Type
    {
        /** 导入导出都生效 */
        ALL(0),
        /** 仅导出 */
        EXPORT(1),
        /** 仅导入 */
        IMPORT(2);
        private final int value;

        Type(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return this.value;
        }
    }

    /**
     * Excel单元格值类型枚举
     */
    public enum ColumnType
    {
        /** 数字类型 */
        NUMERIC(0),
        /** 字符串类型 */
        STRING(1),
        /** 图片类型 */
        IMAGE(2),
        /** 长文本类型 */
        TEXT(3);
        private final int value;

        ColumnType(int value)
        {
            this.value = value;
        }

        public int value()
        {
            return this.value;
        }
    }
}
