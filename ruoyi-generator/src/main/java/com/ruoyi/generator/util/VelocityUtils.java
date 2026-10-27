package com.ruoyi.generator.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.velocity.VelocityContext;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;

/**
 * Apache Velocity 模板渲染辅助工具类
 *
 * <p>在项目中的角色：为代码生成器构建 Velocity 上下文、选择模板列表、
 * 计算生成后的目标路径——是 Velocity 模板渲染流程的"上下文管理器"。</p>
 *
 * <p>Velocity 模板渲染完整流程（由
 * {@link com.ruoyi.generator.service.GenTableServiceImpl} 编排，本工具类提供原子能力）：</p>
 * <ol>
 *   <li>{@link VelocityInitializer#initVelocity} — 初始化 VelocityEngine
 *       （classpath 加载、UTF-8 编码）；</li>
 *   <li>{@link #prepareContext} — 将 {@link GenTable} 配置转化为 {@link VelocityContext}
 *       模板变量（ClassName、moduleName、columns、dicts 等）；</li>
 *   <li>{@link #getTemplateList} — 根据 tplCategory（crud/tree/sub）和
 *       tplWebType（element-plus / element-plus-typescript）选出所有需要的 vm 模板文件；</li>
 *   <li>{@link Velocity#getTemplate} — 加载 vm 模板；</li>
 *   <li>{@code Template.merge(context, writer)} — 渲染到 StringWriter；</li>
 *   <li>{@link #getFileName} — 根据模板名 + 表配置计算生成后的文件目标路径。</li>
 * </ol>
 *
 * <p>本工具类不做任何安全校验——Velocity 引擎本身是安全的（只渲染 classpath 下的静态模板），
 * 且模板中的变量全部来自 GenTable 数据库配置，由管理员在"代码生成"界面受控填写。</p>
 *
 * @author ruoyi
 */
public class VelocityUtils
{
    /** 项目 Java 源码相对路径（拼接在 maven 项目根下） */
    private static final String PROJECT_PATH = "main/java";

    /** MyBatis XML 映射文件相对路径 */
    private static final String MYBATIS_PATH = "main/resources/mapper";

    /** 默认上级菜单 ID（系统工具 = 3） */
    private static final String DEFAULT_PARENT_MENU_ID = "3";

    /** Vue3 Element Plus 前端模板标识 */
    private static final String ELEMENT_PLUS = "element-plus";

    /** Vue3 Element Plus + TypeScript 前端模板标识 */
    private static final String ELEMENT_PLUS_TYPESSRIPT = "element-plus-typescript";

    /**
     * 构建 VelocityContext 模板变量——将 GenTable 所有字段扁平化为模板可访问的变量。
     *
     * <p>设置的关键变量：</p>
     * <ul>
     *   <li>{@code tableName} — 原始表名，用于 SQL 模板；</li>
     *   <li>{@code ClassName / className} — PascalCase / camelCase 类名，Java 源码核心变量；</li>
     *   <li>{@code moduleName / businessName} — 包路径和 URL 路由的关键组成；</li>
     *   <li>{@code tplCategory} — crud/tree/sub，模板逻辑分支依据；</li>
     *   <li>{@code importList} — 需要生成的 import 语句集合（Date/BigDecimal/JsonFormat 等）；</li>
     *   <li>{@code permissionPrefix} — Spring Security 权限前缀（module:business）；</li>
     *   <li>{@code columns} — 完整的 GenTableColumn 列表，供 foreach 遍历；</li>
     *   <li>{@code dicts} — 所有需要字典翻译的 dictType 逗号分隔列表；</li>
     *   <li>{@code treeCode / treeParentCode / treeName} — 树表模板特有；</li>
     *   <li>{@code subTable / subTableName / subTableFkName} — 主子表模板特有。</li>
     * </ul>
     *
     * @param genTable 代码生成主表配置（来自数据库 gen_table）
     * @return 填充完毕的 VelocityContext，可直接传给 {@code Template.merge}
     */
    public static VelocityContext prepareContext(GenTable genTable)
    {
        String moduleName = genTable.getModuleName();
        String businessName = genTable.getBusinessName();
        String packageName = genTable.getPackageName();
        String tplCategory = genTable.getTplCategory();
        String functionName = genTable.getFunctionName();

        VelocityContext velocityContext = new VelocityContext();
        velocityContext.put("tplCategory", genTable.getTplCategory());
        velocityContext.put("tableName", genTable.getTableName());
        velocityContext.put("functionName", StringUtils.isNotEmpty(functionName) ? functionName : "【请填写功能名称】");
        velocityContext.put("ClassName", genTable.getClassName());
        velocityContext.put("className", StringUtils.uncapitalize(genTable.getClassName()));
        velocityContext.put("moduleName", genTable.getModuleName());
        velocityContext.put("BusinessName", StringUtils.capitalize(genTable.getBusinessName()));
        velocityContext.put("businessName", genTable.getBusinessName());
        velocityContext.put("basePackage", getPackagePrefix(packageName));
        velocityContext.put("packageName", packageName);
        velocityContext.put("author", genTable.getFunctionAuthor());
        velocityContext.put("colSpan", getColSpan(genTable.getFormColNum()));
        velocityContext.put("datetime", DateUtils.getDate());
        velocityContext.put("pkColumn", genTable.getPkColumn());
        velocityContext.put("importList", getImportList(genTable));
        velocityContext.put("permissionPrefix", getPermissionPrefix(moduleName, businessName));
        velocityContext.put("columns", genTable.getColumns());
        velocityContext.put("table", genTable);
        velocityContext.put("dicts", getDicts(genTable));
        setExtensionsContext(velocityContext, genTable.getOptions());
        setMenuVelocityContext(velocityContext, genTable);
        if (GenConstants.TPL_TREE.equals(tplCategory))
        {
            setTreeVelocityContext(velocityContext, genTable);
        }
        if (GenConstants.TPL_SUB.equals(tplCategory))
        {
            setSubVelocityContext(velocityContext, genTable);
        }
        return velocityContext;
    }

    /**
     * 处理扩展选项（当前仅 "是否生成详情页"）。
     *
     * @param context VelocityContext
     * @param options JSON 字符串，来自 GenTable.options 字段
     */
    public static void setExtensionsContext(VelocityContext context, String options)
    {
        JSONObject paramsObj = JSONObject.parseObject(options);
        boolean genView = genView(paramsObj);
        context.put("genView", genView);
    }

    /**
     * 设置上级菜单相关的模板变量。
     *
     * @param context VelocityContext
     * @param genTable 代码生成主表
     */
    public static void setMenuVelocityContext(VelocityContext context, GenTable genTable)
    {
        String options = genTable.getOptions();
        JSONObject paramsObj = JSON.parseObject(options);
        String parentMenuId = getParentMenuId(paramsObj);
        context.put("parentMenuId", parentMenuId);
    }

    /**
     * 设置树表模板特有变量（treeCode / treeParentCode / treeName / expandColumn）。
     *
     * @param context VelocityContext
     * @param genTable 代码生成主表
     */
    public static void setTreeVelocityContext(VelocityContext context, GenTable genTable)
    {
        String options = genTable.getOptions();
        JSONObject paramsObj = JSON.parseObject(options);
        String treeCode = getTreecode(paramsObj);
        String treeParentCode = getTreeParentCode(paramsObj);
        String treeName = getTreeName(paramsObj);

        context.put("treeCode", treeCode);
        context.put("treeParentCode", treeParentCode);
        context.put("treeName", treeName);
        context.put("expandColumn", getExpandColumn(genTable));
        if (paramsObj.containsKey(GenConstants.TREE_PARENT_CODE))
        {
            context.put("tree_parent_code", paramsObj.getString(GenConstants.TREE_PARENT_CODE));
        }
        if (paramsObj.containsKey(GenConstants.TREE_NAME))
        {
            context.put("tree_name", paramsObj.getString(GenConstants.TREE_NAME));
        }
    }

    /**
     * 设置主子表模板特有变量（subTable / subTableName / subTableFkName 等）。
     *
     * @param context VelocityContext
     * @param genTable 代码生成主表（其 subTable 字段已在 Service 层填充）
     */
    public static void setSubVelocityContext(VelocityContext context, GenTable genTable)
    {
        GenTable subTable = genTable.getSubTable();
        String subTableName = genTable.getSubTableName();
        String subTableFkName = genTable.getSubTableFkName();
        String subClassName = genTable.getSubTable().getClassName();
        String subTableFkClassName = StringUtils.convertToCamelCase(subTableFkName);

        context.put("subTable", subTable);
        context.put("subTableName", subTableName);
        context.put("subTableFkName", subTableFkName);
        context.put("subTableFkClassName", subTableFkClassName);
        context.put("subTableFkclassName", StringUtils.uncapitalize(subTableFkClassName));
        context.put("subClassName", subClassName);
        context.put("subclassName", StringUtils.uncapitalize(subClassName));
        context.put("subImportList", getImportList(genTable.getSubTable()));
    }

    /**
     * 根据 tplCategory 和 tplWebType 选出所有需要的 vm 模板文件列表。
     *
     * <p>固定包含：domain.java / mapper.java / service.java / serviceImpl.java /
     * controller.java / mapper.xml / sql / api.js(or api.ts)。</p>
     * <p>根据模板类型追加：</p>
     * <ul>
     *   <li>crud → index.vue</li>
     *   <li>tree → index-tree.vue</li>
     *   <li>sub → index.vue + sub-domain.java</li>
     * </ul>
     * <p>根据前端类型追加：</p>
     * <ul>
     *   <li>element-plus-typescript → type.ts + index.ts</li>
     * </ul>
     * <p>如果开启了 genView，还会追加 view.vue 详情页模板。</p>
     *
     * @param table 代码生成主表（含 tplCategory、tplWebType、options 等配置）
     * @return 需要渲染的 vm 模板文件路径列表（相对 classpath）
     */
    public static List<String> getTemplateList(GenTable table)
    {
        String tplWebType = table.getTplWebType();
        String tplCategory = table.getTplCategory();
        JSONObject paramsObj = JSONObject.parseObject(table.getOptions());
        boolean isView = genView(paramsObj);
        String useWebType = "vm/vue";
        String apiTemplate = "vm/js/api.js.vm";
        if (StringUtils.equals(ELEMENT_PLUS, tplWebType))
        {
            useWebType = "vm/vue/v3";
        }
        else if (StringUtils.equals(ELEMENT_PLUS_TYPESSRIPT, tplWebType))
        {
            useWebType = "vm/vue/v3ts";
            apiTemplate = "vm/ts/api.ts.vm";
        }
        List<String> templates = new ArrayList<String>();
        templates.add("vm/java/domain.java.vm");
        templates.add("vm/java/mapper.java.vm");
        templates.add("vm/java/service.java.vm");
        templates.add("vm/java/serviceImpl.java.vm");
        templates.add("vm/java/controller.java.vm");
        templates.add("vm/xml/mapper.xml.vm");
        templates.add("vm/sql/sql.vm");
        templates.add(apiTemplate);
        if (StringUtils.equals(ELEMENT_PLUS_TYPESSRIPT, tplWebType))
        {
            templates.add("vm/ts/type.ts.vm");
            templates.add("vm/ts/index.ts.vm");
        }
        if (GenConstants.TPL_CRUD.equals(tplCategory))
        {
            templates.add(useWebType + "/index.vue.vm");
        }
        else if (GenConstants.TPL_TREE.equals(tplCategory))
        {
            templates.add(useWebType + "/index-tree.vue.vm");
        }
        else if (GenConstants.TPL_SUB.equals(tplCategory))
        {
            templates.add(useWebType + "/index.vue.vm");
            templates.add("vm/java/sub-domain.java.vm");
        }
        if (isView)
        {
            templates.add(useWebType + "/view.vue.vm");
        }
        return templates;
    }

    /**
     * 根据 vm 模板路径 + GenTable 配置，计算生成后的文件目标路径。
     *
     * <p>路径规则示例（{@code com.ruoyi.system.SysUser}）：</p>
     * <ul>
     *   <li>{@code vm/java/domain.java.vm} → {@code main/java/com/ruoyi/system/domain/SysUser.java}</li>
     *   <li>{@code vm/java/mapper.java.vm} → {@code main/java/com/ruoyi/system/mapper/SysUserMapper.java}</li>
     *   <li>{@code vm/xml/mapper.xml.vm} → {@code main/resources/mapper/system/SysUserMapper.xml}</li>
     *   <li>{@code vm/js/api.js.vm} → {@code vue/api/system/user.js}</li>
     *   <li>{@code vm/vue/v3/index.vue.vm} → {@code vue/views/system/user/index.vue}</li>
     * </ul>
     *
     * @param template vm 模板文件路径（相对 classpath）
     * @param genTable 代码生成主表
     * @return 相对于 Maven 项目根的目标文件路径
     */
    public static String getFileName(String template, GenTable genTable)
    {
        // 文件名称
        String fileName = "";
        // 包路径
        String packageName = genTable.getPackageName();
        // 模块名
        String moduleName = genTable.getModuleName();
        // 大写类名
        String className = genTable.getClassName();
        // 业务名称
        String businessName = genTable.getBusinessName();

        String javaPath = PROJECT_PATH + "/" + StringUtils.replace(packageName, ".", "/");
        String mybatisPath = MYBATIS_PATH + "/" + moduleName;
        String vuePath = "vue";

        if (template.contains("domain.java.vm"))
        {
            fileName = StringUtils.format("{}/domain/{}.java", javaPath, className);
        }
        if (template.contains("sub-domain.java.vm") && StringUtils.equals(GenConstants.TPL_SUB, genTable.getTplCategory()))
        {
            fileName = StringUtils.format("{}/domain/{}.java", javaPath, genTable.getSubTable().getClassName());
        }
        else if (template.contains("mapper.java.vm"))
        {
            fileName = StringUtils.format("{}/mapper/{}Mapper.java", javaPath, className);
        }
        else if (template.contains("service.java.vm"))
        {
            fileName = StringUtils.format("{}/service/I{}Service.java", javaPath, className);
        }
        else if (template.contains("serviceImpl.java.vm"))
        {
            fileName = StringUtils.format("{}/service/impl/{}ServiceImpl.java", javaPath, className);
        }
        else if (template.contains("controller.java.vm"))
        {
            fileName = StringUtils.format("{}/controller/{}Controller.java", javaPath, className);
        }
        else if (template.contains("mapper.xml.vm"))
        {
            fileName = StringUtils.format("{}/{}Mapper.xml", mybatisPath, className);
        }
        else if (template.contains("sql.vm"))
        {
            fileName = businessName + "Menu.sql";
        }
        else if (template.contains("api.js.vm"))
        {
            fileName = StringUtils.format("{}/api/{}/{}.js", vuePath, moduleName, businessName);
        }
        else if (template.contains("api.ts.vm"))
        {
            fileName = StringUtils.format("{}/api/{}/{}.ts", vuePath, moduleName, businessName);
        }
        else if (template.contains("type.ts.vm"))
        {
            fileName = StringUtils.format("{}/types/api/{}/{}.ts", vuePath, moduleName, businessName);
        }
        else if (template.contains("index.ts.vm"))
        {
            fileName = StringUtils.format("{}/types/api/index-bak.ts", vuePath);
        }
        else if (template.contains("index.vue.vm"))
        {
            fileName = StringUtils.format("{}/views/{}/{}/index.vue", vuePath, moduleName, businessName);
        }
        else if (template.contains("index-tree.vue.vm"))
        {
            fileName = StringUtils.format("{}/views/{}/{}/index.vue", vuePath, moduleName, businessName);
        }
        else if (template.contains("view.vue.vm"))
        {
            fileName = StringUtils.format("{}/views/{}/{}/view.vue", vuePath, moduleName, businessName);
        }
        return fileName;
    }

    /**
     * 截取包路径的最后一段之前的部分。
     *
     * <p>示例：{@code com.ruoyi.system} → {@code com.ruoyi}</p>
     *
     * @param packageName 完整包名
     * @return 去掉最后一段后的包前缀
     */
    public static String getPackagePrefix(String packageName)
    {
        int lastIndex = packageName.lastIndexOf(".");
        return StringUtils.substring(packageName, 0, lastIndex);
    }

    /**
     * 根据列的 Java 类型推断需要 import 的包集合。
     *
     * <p>规则：</p>
     * <ul>
     *   <li>日期类型 → {@code java.util.Date} + {@code com.fasterxml.jackson.annotation.JsonFormat}；</li>
     *   <li>BigDecimal → {@code java.math.BigDecimal}；</li>
     *   <li>主子表场景 → 追加 {@code java.util.List}。</li>
     * </ul>
     *
     * @param genTable 代码生成主表（含 columns 和 subTable）
     * @return 去重后的 import 包集合
     */
    public static HashSet<String> getImportList(GenTable genTable)
    {
        List<GenTableColumn> columns = genTable.getColumns();
        GenTable subGenTable = genTable.getSubTable();
        HashSet<String> importList = new HashSet<String>();
        if (StringUtils.isNotNull(subGenTable))
        {
            importList.add("java.util.List");
        }
        for (GenTableColumn column : columns)
        {
            if (!column.isSuperColumn() && GenConstants.TYPE_DATE.equals(column.getJavaType()))
            {
                importList.add("java.util.Date");
                importList.add("com.fasterxml.jackson.annotation.JsonFormat");
            }
            else if (!column.isSuperColumn() && GenConstants.TYPE_BIGDECIMAL.equals(column.getJavaType()))
            {
                importList.add("java.math.BigDecimal");
            }
        }
        return importList;
    }

    /**
     * 收集所有需要字典翻译的 dictType——拼接成逗号分隔字符串。
     *
     * <p>仅收集 htmlType 为 select/radio/checkbox 且 dictType 非空的字段。
     * 主子表场景会同时扫描主表和子表的列。</p>
     *
     * @param genTable 代码生成主表
     * @return 去重后的 dictType 集合，逗号分隔（如 {@code 'sys_normal_disable', 'sys_sex'}）
     */
    public static String getDicts(GenTable genTable)
    {
        List<GenTableColumn> columns = genTable.getColumns();
        Set<String> dicts = new HashSet<String>();
        addDicts(dicts, columns);
        if (StringUtils.isNotNull(genTable.getSubTable()))
        {
            List<GenTableColumn> subColumns = genTable.getSubTable().getColumns();
            addDicts(dicts, subColumns);
        }
        return StringUtils.join(dicts, ", ");
    }

    /**
     * 将列列表中的字典类型加入集合。
     *
     * @param dicts   字典类型集合（带引号包裹）
     * @param columns 待扫描的列列表
     */
    public static void addDicts(Set<String> dicts, List<GenTableColumn> columns)
    {
        for (GenTableColumn column : columns)
        {
            if (!column.isSuperColumn() && StringUtils.isNotEmpty(column.getDictType()) && StringUtils.equalsAny(
                    column.getHtmlType(),
                    new String[] { GenConstants.HTML_SELECT, GenConstants.HTML_RADIO, GenConstants.HTML_CHECKBOX }))
            {
                dicts.add("'" + column.getDictType() + "'");
            }
        }
    }

    /**
     * 拼装前端权限前缀：{@code moduleName:businessName}。
     *
     * <p>生成的 Controller 和 Vue 页面中的 {@code @PreAuthorize("hasPermi('...')")}
     * 会使用此前缀，保持前后端权限标识一致。</p>
     *
     * @param moduleName 模块名（如 system）
     * @param businessName 业务名（如 user）
     * @return 权限前缀（如 system:user）
     */
    public static String getPermissionPrefix(String moduleName, String businessName)
    {
        return StringUtils.format("{}:{}", moduleName, businessName);
    }

    /**
     * 从 options JSON 中解析上级菜单 ID——默认为 3（系统工具）。
     *
     * @param paramsObj options 解析后的 JSONObject
     * @return 上级菜单 ID 字符串
     */
    public static String getParentMenuId(JSONObject paramsObj)
    {
        if (StringUtils.isNotEmpty(paramsObj) && paramsObj.containsKey(GenConstants.PARENT_MENU_ID)
                && StringUtils.isNotEmpty(paramsObj.getString(GenConstants.PARENT_MENU_ID)))
        {
            return paramsObj.getString(GenConstants.PARENT_MENU_ID);
        }
        return DEFAULT_PARENT_MENU_ID;
    }

    /**
     * 获取树编码字段名——转换为 camelCase。
     *
     * @param paramsObj options JSONObject
     * @return 树编码字段名（如 {@code treeCode}）
     */
    public static String getTreecode(JSONObject paramsObj)
    {
        if (paramsObj.containsKey(GenConstants.TREE_CODE))
        {
            return StringUtils.toCamelCase(paramsObj.getString(GenConstants.TREE_CODE));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取树父编码字段名——转换为 camelCase。
     *
     * @param paramsObj options JSONObject
     * @return 树父编码字段名
     */
    public static String getTreeParentCode(JSONObject paramsObj)
    {
        if (paramsObj.containsKey(GenConstants.TREE_PARENT_CODE))
        {
            return StringUtils.toCamelCase(paramsObj.getString(GenConstants.TREE_PARENT_CODE));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 扩展功能：判断是否需要生成详情页。
     *
     * @param paramsObj options JSONObject
     * @return true 表示额外生成 view.vue 详情页
     */
    public static boolean genView(JSONObject paramsObj)
    {
        if (StringUtils.isNotNull(paramsObj) && paramsObj.containsKey(GenConstants.GEN_VIEW))
        {
            return paramsObj.getBoolean(GenConstants.GEN_VIEW);
        }
        return false;
    }

    /**
     * 获取树名称字段名——转换为 camelCase。
     *
     * @param paramsObj options JSONObject
     * @return 树名称字段名
     */
    public static String getTreeName(JSONObject paramsObj)
    {
        if (paramsObj.containsKey(GenConstants.TREE_NAME))
        {
            return StringUtils.toCamelCase(paramsObj.getString(GenConstants.TREE_NAME));
        }
        return StringUtils.EMPTY;
    }

    /**
     * 获取列表中树展开按钮应该放在哪一列。
     *
     * <p>遍历 GenTable.columns 中被标记为"列表显示"的列，
     * 直到遇到 treeName 指定的列为止，计数即为展开按钮所在列。</p>
     *
     * @param genTable 代码生成主表（含 options.treeName）
     * @return 展开按钮列序号（从 0 开始）
     */
    public static int getExpandColumn(GenTable genTable)
    {
        String options = genTable.getOptions();
        JSONObject paramsObj = JSON.parseObject(options);
        String treeName = paramsObj.getString(GenConstants.TREE_NAME);
        int num = 0;
        for (GenTableColumn column : genTable.getColumns())
        {
            if (column.isList())
            {
                num++;
                String columnName = column.getColumnName();
                if (columnName.equals(treeName))
                {
                    break;
                }
            }
        }
        return num;
    }

    /**
     * 将表单布局方式转为 Element Plus 的 el-col span 值。
     *
     * <p>布局规则：1=单列(24)、2=双列(12)、3=三列(8)。</p>
     *
     * @param formColNum 表单布局方式
     * @return el-col span 数值字符串
     */
    public static String getColSpan(int formColNum)
    {
        if (formColNum == 2)
        {
            return "12";
        }
        else if (formColNum == 3)
        {
            return "8";
        }
        return "24";
    }
}
