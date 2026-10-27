package com.ruoyi.common.constant;

/**
 * 代码生成器通用常量类
 * 
 * 定义RuoYi代码生成模块使用的各类模板标识、数据库类型映射、
 * 前端页面控件类型、Java数据类型映射、查询方式等常量值。
 * 
 * @author ruoyi
 */
public class GenConstants
{
    /**
     * 单表CRUD模板标识，生成基础的增删改查代码
     */
    public static final String TPL_CRUD = "crud";

    /**
     * 树表模板标识，生成树形结构的增删改查代码（如部门、菜单）
     */
    public static final String TPL_TREE = "tree";

    /**
     * 主子表模板标识，生成包含主表和子表关联的增删改查代码
     */
    public static final String TPL_SUB = "sub";

    /**
     * 树表结构中存储层级编码的字段名常量
     */
    public static final String TREE_CODE = "treeCode";

    /**
     * 树表结构中存储父级编码的字段名常量
     */
    public static final String TREE_PARENT_CODE = "treeParentCode";

    /**
     * 树表结构中存储节点名称的字段名常量
     */
    public static final String TREE_NAME = "treeName";

    /**
     * 代码生成时上级菜单ID字段名
     */
    public static final String PARENT_MENU_ID = "parentMenuId";

    /**
     * 代码生成时上级菜单名称字段名
     */
    public static final String PARENT_MENU_NAME = "parentMenuName";

    /**
     * 代码生成详情页开关常量，控制是否生成详情页模板
     */
    public static final String GEN_VIEW = "genView";

    /**
     * 数据库字符串类型集合：char, varchar, nvarchar, varchar2
     */
    public static final String[] COLUMNTYPE_STR = { "char", "varchar", "nvarchar", "varchar2" };

    /**
     * 数据库文本类型集合：tinytext, text, mediumtext, longtext
     */
    public static final String[] COLUMNTYPE_TEXT = { "tinytext", "text", "mediumtext", "longtext" };

    /**
     * 数据库时间类型集合：datetime, time, date, timestamp
     */
    public static final String[] COLUMNTYPE_TIME = { "datetime", "time", "date", "timestamp" };

    /**
     * 数据库数字类型集合：tinyint, smallint, mediumint, int, number, integer, bit, bigint, float, double, decimal
     */
    public static final String[] COLUMNTYPE_NUMBER = { "tinyint", "smallint", "mediumint", "int", "number", "integer",
            "bit", "bigint", "float", "double", "decimal" };

    /**
     * 代码生成时页面不需要生成编辑表单的字段（系统级字段）：id, create_by, create_time, del_flag
     */
    public static final String[] COLUMNNAME_NOT_EDIT = { "id", "create_by", "create_time", "del_flag" };

    /**
     * 代码生成时页面不需要生成列表展示的字段（系统级字段）
     */
    public static final String[] COLUMNNAME_NOT_LIST = { "id", "create_by", "create_time", "del_flag", "update_by",
            "update_time" };

    /**
     * 代码生成时页面不需要生成查询条件的字段（系统级字段和备注）
     */
    public static final String[] COLUMNNAME_NOT_QUERY = { "id", "create_by", "create_time", "del_flag", "update_by",
            "update_time", "remark" };

    /**
     * Entity基类标准字段集合：createBy, createTime, updateBy, updateTime, remark
     */
    public static final String[] BASE_ENTITY = { "createBy", "createTime", "updateBy", "updateTime", "remark" };

    /**
     * TreeEntity基类标准字段集合：parentName, parentId, orderNum, ancestors, children
     */
    public static final String[] TREE_ENTITY = { "parentName", "parentId", "orderNum", "ancestors", "children" };

    // ========== 前端HTML控件类型 ==========
    /** 前端文本框控件标识 */
    public static final String HTML_INPUT = "input";

    /** 前端文本域控件标识 */
    public static final String HTML_TEXTAREA = "textarea";

    /** 前端下拉选择控件标识 */
    public static final String HTML_SELECT = "select";

    /** 前端单选框控件标识 */
    public static final String HTML_RADIO = "radio";

    /** 前端复选框控件标识 */
    public static final String HTML_CHECKBOX = "checkbox";

    /** 前端日期选择控件标识 */
    public static final String HTML_DATETIME = "datetime";

    /** 前端图片上传控件标识 */
    public static final String HTML_IMAGE_UPLOAD = "imageUpload";

    /** 前端文件上传控件标识 */
    public static final String HTML_FILE_UPLOAD = "fileUpload";

    /** 前端富文本编辑器控件标识 */
    public static final String HTML_EDITOR = "editor";

    // ========== Java数据类型映射 ==========
    /** Java字符串类型标识 */
    public static final String TYPE_STRING = "String";

    /** Java整型标识 */
    public static final String TYPE_INTEGER = "Integer";

    /** Java长整型标识 */
    public static final String TYPE_LONG = "Long";

    /** Java双精度浮点型标识 */
    public static final String TYPE_DOUBLE = "Double";

    /** Java高精度计算类型标识 */
    public static final String TYPE_BIGDECIMAL = "BigDecimal";

    /** Java日期时间类型标识 */
    public static final String TYPE_DATE = "Date";

    // ========== 查询条件类型 ==========
    /** 模糊查询类型LIKE */
    public static final String QUERY_LIKE = "LIKE";

    /** 精确相等查询类型EQ */
    public static final String QUERY_EQ = "EQ";

    /** 字段为必填项标识值 "1" */
    public static final String REQUIRE = "1";
}
