package com.ruoyi.generator.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.GenConstants;
import com.ruoyi.common.core.text.CharsetKit;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;
import com.ruoyi.generator.mapper.GenTableColumnMapper;
import com.ruoyi.generator.mapper.GenTableMapper;
import com.ruoyi.generator.util.GenUtils;
import com.ruoyi.generator.util.VelocityInitializer;
import com.ruoyi.generator.util.VelocityUtils;

/**
 * 代码生成主业务服务实现
 *
 * <p>在项目中的角色：代码生成器的"编排中枢"——串联 GenUtils（命名推导）、
 * VelocityInitializer（模板引擎初始化）、VelocityUtils（上下文构建/模板选择/路径计算）、
 * GenTableMapper（持久化）四个原子能力，
 * 完成"导入表 → 预览代码 → 生成 ZIP / 写文件 → 同步数据库"全流程。</p>
 *
 * <p>安全设计：</p>
 * <ul>
 *   <li>所有生成的代码模板都从 classpath 加载（{@link VelocityInitializer}），
 *       不可注入外部模板；</li>
 *   <li>生成自定义路径 {@code genPath} 是否允许覆盖由 {@code GenConfig.allowOverwrite} 开关控制；</li>
 *   <li>"创建表"接口由管理员（admin 角色）独占，且走 SQLUtils 解析 + SqlUtil.filterKeyword 过滤；</li>
 *   <li>模板变量全部来自数据库配置（管理员受控填写），无 eval 或 Runtime 注入风险。</li>
 * </ul>
 *
 * @author ruoyi
 */
@Service
public class GenTableServiceImpl implements IGenTableService
{
    private static final Logger log = LoggerFactory.getLogger(GenTableServiceImpl.class);

    @Autowired
    private GenTableMapper genTableMapper;

    @Autowired
    private GenTableColumnMapper genTableColumnMapper;

    /**
     * 根据 tableId 查询代码生成配置——附加解析 options 扩展字段。
     *
     * @param id gen_table 主键
     * @return 完整的 GenTable（含 treeCode/parentMenuId/isView 等扩展字段）
     */
    @Override
    public GenTable selectGenTableById(Long id)
    {
        GenTable genTable = genTableMapper.selectGenTableById(id);
        setTableFromOptions(genTable);
        return genTable;
    }

    /**
     * 分页查询代码生成列表。
     *
     * @param genTable 查询条件
     * @return 代码生成配置集合
     */
    @Override
    public List<GenTable> selectGenTableList(GenTable genTable)
    {
        return genTableMapper.selectGenTableList(genTable);
    }

    /**
     * 查询数据库中所有表（供"导入表结构"界面选择）。
     *
     * @param genTable 查询条件（支持模糊匹配表名、表注释）
     * @return 数据库中的表集合
     */
    @Override
    public List<GenTable> selectDbTableList(GenTable genTable)
    {
        return genTableMapper.selectDbTableList(genTable);
    }

    /**
     * 根据表名批量查询数据库元信息。
     *
     * @param tableNames 待导入的数据库表名数组
     * @return 表集合
     */
    @Override
    public List<GenTable> selectDbTableListByNames(String[] tableNames)
    {
        return genTableMapper.selectDbTableListByNames(tableNames);
    }

    /**
     * 查询所有代码生成配置（供编辑页面主子表联动选择）。
     *
     * @return 全部 GenTable 集合
     */
    @Override
    public List<GenTable> selectGenTableAll()
    {
        return genTableMapper.selectGenTableAll();
    }

    /**
     * 更新代码生成配置（表 + 列 + options）。
     *
     * @param genTable 更新后的配置（含 params 临时字段会被序列化为 options）
     */
    @Override
    @Transactional
    public void updateGenTable(GenTable genTable)
    {
        String options = JSON.toJSONString(genTable.getParams());
        genTable.setOptions(options);
        int row = genTableMapper.updateGenTable(genTable);
        if (row > 0)
        {
            for (GenTableColumn genTableColumn : genTable.getColumns())
            {
                genTableColumnMapper.updateGenTableColumn(genTableColumn);
            }
        }
    }

    /**
     * 删除代码生成配置（主表 + 所有关联列）。
     *
     * @param tableIds 待删除的 GenTable 主键数组
     */
    @Override
    @Transactional
    public void deleteGenTableByIds(Long[] tableIds)
    {
        genTableMapper.deleteGenTableByIds(tableIds);
        genTableColumnMapper.deleteGenTableColumnByIds(tableIds);
    }

    /**
     * 在数据库中执行 CREATE TABLE DDL。
     *
     * <p>返回值约定：Mapper 返回 0 表示创建成功（DDL 不影响行数），
     * 因此 Service 层转为 {@code createTable(...) == 0 → true}。</p>
     *
     * @param sql 已过滤关键字的 CREATE TABLE 语句
     * @return true 表示创建成功
     */
    @Override
    public boolean createTable(String sql)
    {
        return genTableMapper.createTable(sql) == 0;
    }

    /**
     * 从数据库导入表结构——批量创建 GenTable + GenTableColumn 记录。
     *
     * <p>完整流程：</p>
     * <ol>
     *   <li>为每张表调 {@link GenUtils#initTable} 推导类名/模块名/业务名/作者等；</li>
     *   <li>插入 gen_table；</li>
     *   <li>从数据库元信息读取所有列；</li>
     *   <li>调 {@link GenUtils#initColumnField} 自动推导 Java 类型/HTML 控件等；</li>
     *   <li>逐条插入 gen_table_column。</li>
     * </ol>
     *
     * @param tableList   从数据库查询到的表元信息
     * @param tplWebType  前端模板类型（element-plus / element-plus-typescript）
     * @param operName    操作人用户名
     */
    @Override
    @Transactional
    public void importGenTable(List<GenTable> tableList, String tplWebType, String operName)
    {
        try
        {
            for (GenTable table : tableList)
            {
                String tableName = table.getTableName();
                table.setTplWebType(tplWebType);
                GenUtils.initTable(table, operName);
                int row = genTableMapper.insertGenTable(table);
                if (row > 0)
                {
                    // 保存列信息
                    List<GenTableColumn> genTableColumns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
                    for (GenTableColumn column : genTableColumns)
                    {
                        GenUtils.initColumnField(column, table);
                        genTableColumnMapper.insertGenTableColumn(column);
                    }
                }
            }
        }
        catch (Exception e)
        {
            throw new ServiceException("导入失败：" + e.getMessage());
        }
    }

    /**
     * 预览代码（返回 Map<模板路径, 渲染结果>）。
     *
     * <p>Velocity 模板渲染核心流程：</p>
     * <ol>
     *   <li>读取 GenTable + 填充 subTable + 填充 pkColumn；</li>
     *   <li>{@link VelocityInitializer#initVelocity} 初始化引擎；</li>
     *   <li>{@link VelocityUtils#prepareContext} 构建 VelocityContext；</li>
     *   <li>{@link VelocityUtils#getTemplateList} 选出所有需要渲染的模板；</li>
     *   <li>逐个 {@code Template.merge(context, sw)} 渲染到 StringWriter；</li>
     *   <li>Map 收集返回（key=模板路径，value=渲染结果）。</li>
     * </ol>
     *
     * @param tableId gen_table 主键
     * @return 模板路径 → 渲染后代码的映射
     */
    @Override
    public Map<String, String> previewCode(Long tableId)
    {
        Map<String, String> dataMap = new LinkedHashMap<>();
        // 查询表信息
        GenTable table = genTableMapper.selectGenTableById(tableId);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);
        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table);
        for (String template : templates)
        {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            dataMap.put(template, sw.toString());
        }
        return dataMap;
    }

    /**
     * 生成代码（下载方式）——单表便捷方法，委托批量方法。
     *
     * @param tableName 业务表名
     * @return ZIP 压缩包字节流
     */
    @Override
    public byte[] downloadCode(String tableName)
    {
        return downloadCode(new String[] { tableName });
    }

    /**
     * 生成代码（自定义路径写文件方式）。
     *
     * <p>从模板渲染到本地文件。注意：过滤掉 sql.vm / api.* / vue 模板——
     * 只写 Java/XML 后端源码，前端文件需要另行处理。</p>
     *
     * @param tableName 业务表名
     */
    @Override
    public void generatorCode(String tableName)
    {
        // 查询表信息
        GenTable table = genTableMapper.selectGenTableByName(tableName);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table);
        for (String template : templates)
        {
            if (!StringUtils.containsAny(template, "sql.vm", "api.js.vm", "api.ts.vm", "type.ts.vm", "index.ts.vm", "index.vue.vm", "index-tree.vue.vm", "view.vue.vm"))
            {
                // 渲染模板
                StringWriter sw = new StringWriter();
                Template tpl = Velocity.getTemplate(template, Constants.UTF8);
                tpl.merge(context, sw);
                try
                {
                    String path = getGenPath(table, template);
                    FileUtils.writeStringToFile(new File(path), sw.toString(), CharsetKit.UTF_8);
                }
                catch (IOException e)
                {
                    throw new ServiceException("渲染模板失败，表名：" + table.getTableName());
                }
            }
        }
    }

    /**
     * 同步数据库元信息——将数据库最新列定义回填到 gen_table_column。
     *
     * <p>核心逻辑：比对 gen_table_column 旧列 vs 数据库新列：</p>
     * <ul>
     *   <li>数据库有、旧表没有 → 插入新列；</li>
     *   <li>两边都有 → 更新（保留用户已调整的 dictType/queryType/htmlType/isRequired 等字段）；</li>
     *   <li>旧表有、数据库没有 → 删除（数据库已删除的列）。</li>
     * </ul>
     *
     * @param tableName 业务表名
     */
    @Override
    @Transactional
    public void synchDb(String tableName)
    {
        GenTable table = genTableMapper.selectGenTableByName(tableName);
        List<GenTableColumn> tableColumns = table.getColumns();
        Map<String, GenTableColumn> tableColumnMap = tableColumns.stream().collect(Collectors.toMap(GenTableColumn::getColumnName, Function.identity()));

        List<GenTableColumn> dbTableColumns = genTableColumnMapper.selectDbTableColumnsByName(tableName);
        if (StringUtils.isEmpty(dbTableColumns))
        {
            throw new ServiceException("同步数据失败，原表结构不存在");
        }
        List<String> dbTableColumnNames = dbTableColumns.stream().map(GenTableColumn::getColumnName).collect(Collectors.toList());

        dbTableColumns.forEach(column -> {
            GenUtils.initColumnField(column, table);
            if (tableColumnMap.containsKey(column.getColumnName()))
            {
                GenTableColumn prevColumn = tableColumnMap.get(column.getColumnName());
                column.setColumnId(prevColumn.getColumnId());
                if (column.isList())
                {
                    // 如果是列表，继续保留查询方式/字典类型选项
                    column.setDictType(prevColumn.getDictType());
                    column.setQueryType(prevColumn.getQueryType());
                }
                if (StringUtils.isNotEmpty(prevColumn.getIsRequired()) && !column.isPk()
                        && (column.isInsert() || column.isEdit())
                        && ((column.isUsableColumn()) || (!column.isSuperColumn())))
                {
                    // 如果是(新增/修改&非主键/非忽略及父属性)，继续保留必填/显示类型选项
                    column.setIsRequired(prevColumn.getIsRequired());
                    column.setHtmlType(prevColumn.getHtmlType());
                }
                genTableColumnMapper.updateGenTableColumn(column);
            }
            else
            {
                genTableColumnMapper.insertGenTableColumn(column);
            }
        });

        List<GenTableColumn> delColumns = tableColumns.stream().filter(column -> !dbTableColumnNames.contains(column.getColumnName())).collect(Collectors.toList());
        if (StringUtils.isNotEmpty(delColumns))
        {
            genTableColumnMapper.deleteGenTableColumns(delColumns);
        }
    }

    /**
     * 批量生成代码（下载方式）——多表打包成一个 ZIP 返回。
     *
     * <p>特别处理：TypeScript 场景的 {@code index-bak.ts} 需要合并所有表的
     * {@code export * from ...} 行，使用 Map 收集后一次性写入 ZIP。</p>
     *
     * @param tableNames 业务表名数组
     * @return ZIP 压缩包字节流
     */
    @Override
    public byte[] downloadCode(String[] tableNames)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(outputStream);
        Map<String, StringBuffer> typeFiles = new HashMap<>();
        for (String tableName : tableNames)
        {
            generatorCode(tableName, zip, typeFiles);
        }
        for (Map.Entry<String, StringBuffer> entry : typeFiles.entrySet())
        {
            writeToZip(zip, entry.getKey(), entry.getValue().toString());
        }
        IOUtils.closeQuietly(zip);
        return outputStream.toByteArray();
    }

    /**
     * 查询表信息并渲染模板后写入 ZIP 流——单表内部方法。
     *
     * <p>与 {@link #previewCode} 基本相同，差异：</p>
     * <ul>
     *   <li>按 {@link VelocityUtils#getFileName} 计算 ZIP 内条目路径；</li>
     *   <li>TypeScript 的 index-bak.ts 做特殊合并处理；</li>
     *   <li>其余文件直接调 {@link #writeToZip}。</li>
     * </ul>
     *
     * @param tableName  业务表名
     * @param zip        ZIP 输出流
     * @param typeFiles  TypeScript 特殊文件合并 Map（key=文件名, value=合并后的 StringBuffer）
     */
    private void generatorCode(String tableName, ZipOutputStream zip, Map<String, StringBuffer> typeFiles)
    {
        // 查询表信息
        GenTable table = genTableMapper.selectGenTableByName(tableName);
        // 设置主子表信息
        setSubTable(table);
        // 设置主键列信息
        setPkColumn(table);

        VelocityInitializer.initVelocity();

        VelocityContext context = VelocityUtils.prepareContext(table);

        // 获取模板列表
        List<String> templates = VelocityUtils.getTemplateList(table);
        for (String template : templates)
        {
            // 渲染模板
            StringWriter sw = new StringWriter();
            Template tpl = Velocity.getTemplate(template, Constants.UTF8);
            tpl.merge(context, sw);
            String fileName = VelocityUtils.getFileName(template, table);
            // index-bak.ts 模版，追加内容
            if (fileName.contains("index-bak.ts"))
            {
                if (!typeFiles.containsKey(fileName))
                {
                    typeFiles.put(fileName, new StringBuffer(sw.toString()));
                }
                else
                {
                    Arrays.stream(sw.toString().split("\n")).filter(line -> line.startsWith("export * from")).forEach(line -> typeFiles.get(fileName).append("\n").append(line));
                }
            }
            else
            {
                // 其他文件正常添加
                writeToZip(zip, fileName, sw.toString());
            }
        }
    }

    /**
     * 将渲染后的代码写入 ZIP 条目。
     *
     * @param zip     ZIP 输出流
     * @param fileName ZIP 条目名称（相对路径）
     * @param content 渲染后的源码字符串
     */
    private void writeToZip(ZipOutputStream zip, String fileName, String content)
    {
        try
        {
            zip.putNextEntry(new ZipEntry(fileName));
            IOUtils.write(content, zip, Constants.UTF8);
            zip.flush();
            zip.closeEntry();
        }
        catch (IOException e)
        {
            log.error("写入ZIP文件失败，文件名: " + fileName, e);
        }
    }

    /**
     * 编辑保存校验——树表和主子表必须填完整关联字段。
     *
     * @param genTable 编辑后的配置
     * @throws ServiceException 校验失败时抛出
     */
    @Override
    public void validateEdit(GenTable genTable)
    {
        if (GenConstants.TPL_TREE.equals(genTable.getTplCategory()))
        {
            String options = JSON.toJSONString(genTable.getParams());
            JSONObject paramsObj = JSON.parseObject(options);
            if (StringUtils.isEmpty(paramsObj.getString(GenConstants.TREE_CODE)))
            {
                throw new ServiceException("树编码字段不能为空");
            }
            else if (StringUtils.isEmpty(paramsObj.getString(GenConstants.TREE_PARENT_CODE)))
            {
                throw new ServiceException("树父编码字段不能为空");
            }
            else if (StringUtils.isEmpty(paramsObj.getString(GenConstants.TREE_NAME)))
            {
                throw new ServiceException("树名称字段不能为空");
            }
        }
        else if (GenConstants.TPL_SUB.equals(genTable.getTplCategory()))
        {
            if (StringUtils.isEmpty(genTable.getSubTableName()))
            {
                throw new ServiceException("关联子表的表名不能为空");
            }
            else if (StringUtils.isEmpty(genTable.getSubTableFkName()))
            {
                throw new ServiceException("子表关联的外键名不能为空");
            }
        }
    }

    /**
     * 填充 GenTable 的主键列信息——第一个 isPk=1 的列，若没有则 fallback 到第一列。
     *
     * <p>主子表场景会同时填充子表的 pkColumn。</p>
     *
     * @param table 代码生成主表
     */
    public void setPkColumn(GenTable table)
    {
        for (GenTableColumn column : table.getColumns())
        {
            if (column.isPk())
            {
                table.setPkColumn(column);
                break;
            }
        }
        if (StringUtils.isNull(table.getPkColumn()))
        {
            table.setPkColumn(table.getColumns().get(0));
        }
        if (GenConstants.TPL_SUB.equals(table.getTplCategory()))
        {
            for (GenTableColumn column : table.getSubTable().getColumns())
            {
                if (column.isPk())
                {
                    table.getSubTable().setPkColumn(column);
                    break;
                }
            }
            if (StringUtils.isNull(table.getSubTable().getPkColumn()))
            {
                table.getSubTable().setPkColumn(table.getSubTable().getColumns().get(0));
            }
        }
    }

    /**
     * 填充主子表关联信息——根据 subTableName 查 GenTableMapper 取子表完整配置。
     *
     * @param table 主表配置（其 subTable 字段会被填充）
     */
    public void setSubTable(GenTable table)
    {
        String subTableName = table.getSubTableName();
        if (StringUtils.isNotEmpty(subTableName))
        {
            table.setSubTable(genTableMapper.selectGenTableByName(subTableName));
        }
    }

    /**
     * 解析 GenTable.options JSON 字符串，回填到各独立字段（treeCode/parentMenuId/isView 等）。
     *
     * <p>options 是"其它生成选项"的 JSON 序列化，例如：
     * {@code {"treeCode":"treeId","treeParentCode":"parentId","treeName":"label",
     * "parentMenuId":3,"parentMenuName":"系统工具","genView":true}}。</p>
     *
     * @param genTable 需要回填扩展字段的主表
     */
    public void setTableFromOptions(GenTable genTable)
    {
        JSONObject paramsObj = JSON.parseObject(genTable.getOptions());
        if (StringUtils.isNotNull(paramsObj))
        {
            String treeCode = paramsObj.getString(GenConstants.TREE_CODE);
            String treeParentCode = paramsObj.getString(GenConstants.TREE_PARENT_CODE);
            String treeName = paramsObj.getString(GenConstants.TREE_NAME);
            Long parentMenuId = paramsObj.getLongValue(GenConstants.PARENT_MENU_ID);
            String parentMenuName = paramsObj.getString(GenConstants.PARENT_MENU_NAME);
            boolean isView = paramsObj.getBooleanValue(GenConstants.GEN_VIEW);

            genTable.setTreeCode(treeCode);
            genTable.setTreeParentCode(treeParentCode);
            genTable.setTreeName(treeName);
            genTable.setParentMenuId(parentMenuId);
            genTable.setParentMenuName(parentMenuName);
            genTable.setView(isView);
        }
    }

    /**
     * 计算自定义路径生成时的文件输出地址。
     *
     * <p>当 {@code genPath="/"} 时默认输出到 {@code System.getProperty("user.dir")}；
     * 否则直接拼接 genPath + 模板文件名。</p>
     *
     * @param table    代码生成主表
     * @param template vm 模板路径
     * @return 绝对文件路径
     */
    public static String getGenPath(GenTable table, String template)
    {
        String genPath = table.getGenPath();
        if (StringUtils.equals(genPath, "/"))
        {
            return System.getProperty("user.dir") + File.separator + "src" + File.separator + VelocityUtils.getFileName(template, table);
        }
        return genPath + File.separator + VelocityUtils.getFileName(template, table);
    }
}