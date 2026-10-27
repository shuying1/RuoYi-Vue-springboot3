package com.ruoyi.generator.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.dialect.mysql.ast.statement.MySqlCreateTableStatement;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.sql.SqlUtil;
import com.ruoyi.generator.config.GenConfig;
import com.ruoyi.generator.domain.GenTable;
import com.ruoyi.generator.domain.GenTableColumn;
import com.ruoyi.generator.service.IGenTableColumnService;
import com.ruoyi.generator.service.IGenTableService;

/**
 * 代码生成器 REST 控制器
 *
 * <p>在项目中的角色：暴露"代码生成"功能的全部 HTTP 接口，路由 {@code /tool/gen}，
 * 覆盖：数据库元信息查询 → 导入表结构 → 预览代码 → 生成 ZIP 下载 → 自定义路径写文件 → 同步数据库。</p>
 *
 * <p>安全与权限：</p>
 * <ul>
 *   <li>所有接口需要特定 {@code tool:gen:*} 权限；</li>
 *   <li>{@code createTableSave}（从 SQL 创建表）限定 {@code admin} 角色，
 *       且 SQL 先经 {@link com.ruoyi.common.utils.sql.SqlUtil#filterKeyword} 过滤关键字、
 *       再由 {@link com.alibaba.druid.sql.SQLUtils} 解析，只接受 {@code CREATE TABLE} DDL；</li>
 *   <li>自定义路径写文件由 {@link GenConfig#isAllowOverwrite()} 全局开关控制，
 *       默认关闭，防止误覆盖本地项目。</li>
 * </ul>
 *
 * <p>数据流向：Controller → GenTableServiceImpl → VelocityUtils + VelocityInitializer
 * → vm 模板渲染 → ZIP / 本地文件。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/tool/gen")
public class GenController extends BaseController
{
    @Autowired
    private IGenTableService genTableService;

    @Autowired
    private IGenTableColumnService genTableColumnService;

    /**
     * 查询代码生成列表（分页）。
     *
     * @param genTable 查询条件
     * @return 分页后的代码生成配置列表
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping("/list")
    public TableDataInfo genList(GenTable genTable)
    {
        startPage();
        List<GenTable> list = genTableService.selectGenTableList(genTable);
        return getDataTable(list);
    }

    /**
     * 获取单条代码生成配置详情。
     *
     * @param tableId gen_table 主键
     * @return 包含主表、列、所有表列表的 Map（info/rows/tables 三个 key）
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:query')")
    @GetMapping(value = "/{tableId}")
    public AjaxResult getInfo(@PathVariable Long tableId)
    {
        GenTable table = genTableService.selectGenTableById(tableId);
        List<GenTable> tables = genTableService.selectGenTableAll();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("info", table);
        map.put("rows", list);
        map.put("tables", tables);
        return success(map);
    }

    /**
     * 查询数据库中所有表（供"导入表结构"界面选择）。
     *
     * @param genTable 查询条件
     * @return 数据库表列表
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping("/db/list")
    public TableDataInfo dataList(GenTable genTable)
    {
        startPage();
        List<GenTable> list = genTableService.selectDbTableList(genTable);
        return getDataTable(list);
    }

    /**
     * 查询指定代码生成配置的列信息。
     *
     * @param tableId gen_table 主键
     * @return 列列表
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:list')")
    @GetMapping(value = "/column/{tableId}")
    public TableDataInfo columnList(Long tableId)
    {
        TableDataInfo dataInfo = new TableDataInfo();
        List<GenTableColumn> list = genTableColumnService.selectGenTableColumnListByTableId(tableId);
        dataInfo.setRows(list);
        dataInfo.setTotal(list.size());
        return dataInfo;
    }

    /**
     * 导入数据库表结构到代码生成器。
     *
     * @param tables    待导入的数据库表名（逗号分隔字符串）
     * @param tplWebType 前端模板类型（element-plus / element-plus-typescript）
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:import')")
    @Log(title = "代码生成", businessType = BusinessType.IMPORT)
    @PostMapping("/importTable")
    public AjaxResult importTableSave(@RequestParam("tables") String tables, @RequestParam("tplWebType") String tplWebType)
    {
        String[] tableNames = Convert.toStrArray(tables);
        // 查询表信息
        List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames);
        genTableService.importGenTable(tableList, tplWebType, SecurityUtils.getUsername());
        return success();
    }

    /**
     * 从 SQL 语句创建表并导入。
     *
     * <p>仅 admin 角色可用。内部先用 Druid SQLUtils 解析，只接受
     * {@code MySqlCreateTableStatement}，然后调 GenTableMapper.createTable 执行 DDL，
     * 成功后再走 importGenTable 流程。</p>
     *
     * @param sql         CREATE TABLE DDL（已过滤关键字）
     * @param tplWebType  前端模板类型
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasRole('admin')")
    @Log(title = "创建表", businessType = BusinessType.OTHER)
    @PostMapping("/createTable")
    public AjaxResult createTableSave(@RequestParam("sql") String sql, @RequestParam("tplWebType") String tplWebType)
    {
        try
        {
            SqlUtil.filterKeyword(sql);
            List<SQLStatement> sqlStatements = SQLUtils.parseStatements(sql, DbType.mysql);
            List<String> tableNames = new ArrayList<>();
            for (SQLStatement sqlStatement : sqlStatements)
            {
                if (sqlStatement instanceof MySqlCreateTableStatement)
                {
                    MySqlCreateTableStatement createTableStatement = (MySqlCreateTableStatement) sqlStatement;
                    if (genTableService.createTable(createTableStatement.toString()))
                    {
                        String tableName = createTableStatement.getTableName().replaceAll("`", "");
                        tableNames.add(tableName);
                    }
                }
            }
            List<GenTable> tableList = genTableService.selectDbTableListByNames(tableNames.toArray(new String[tableNames.size()]));
            String operName = SecurityUtils.getUsername();
            genTableService.importGenTable(tableList, tplWebType, operName);
            return AjaxResult.success();
        }
        catch (Exception e)
        {
            logger.error(e.getMessage(), e);
            return AjaxResult.error("创建表结构异常");
        }
    }

    /**
     * 保存代码生成配置的修改。
     *
     * @param genTable 修改后的配置（含 columns）
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:edit')")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult editSave(@Validated @RequestBody GenTable genTable)
    {
        genTableService.validateEdit(genTable);
        genTableService.updateGenTable(genTable);
        return success();
    }

    /**
     * 删除代码生成配置。
     *
     * @param tableIds 待删除的 gen_table 主键数组
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:remove')")
    @Log(title = "代码生成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tableIds}")
    public AjaxResult remove(@PathVariable Long[] tableIds)
    {
        genTableService.deleteGenTableByIds(tableIds);
        return success();
    }

    /**
     * 预览代码——返回所有模板的渲染结果。
     *
     * @param tableId gen_table 主键
     * @return Map<模板路径, 渲染后的源码>
     * @throws IOException 模板渲染异常
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:preview')")
    @GetMapping("/preview/{tableId}")
    public AjaxResult preview(@PathVariable("tableId") Long tableId) throws IOException
    {
        Map<String, String> dataMap = genTableService.previewCode(tableId);
        return success(dataMap);
    }

    /**
     * 生成单个表的代码并返回 ZIP 下载流。
     *
     * @param response HTTP 响应，用于写出 ZIP 流
     * @param tableName 业务表名
     * @throws IOException 写出异常
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:code')")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/download/{tableName}")
    public void download(HttpServletResponse response, @PathVariable("tableName") String tableName) throws IOException
    {
        byte[] data = genTableService.downloadCode(tableName);
        genCode(response, data);
    }

    /**
     * 生成单个表的代码到自定义本地路径。
     *
     * <p>是否允许覆盖由 {@link GenConfig#isAllowOverwrite()} 全局控制。</p>
     *
     * @param tableName 业务表名
     * @return 操作结果（若不允许覆盖则返回 error）
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:code')")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/genCode/{tableName}")
    public AjaxResult genCode(@PathVariable("tableName") String tableName)
    {
        if (!GenConfig.isAllowOverwrite())
        {
            return AjaxResult.error("【系统预设】不允许生成文件覆盖到本地");
        }
        genTableService.generatorCode(tableName);
        return success();
    }

    /**
     * 同步数据库最新表结构到代码生成配置。
     *
     * @param tableName 业务表名
     * @return 操作结果
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:edit')")
    @Log(title = "代码生成", businessType = BusinessType.UPDATE)
    @GetMapping("/synchDb/{tableName}")
    public AjaxResult synchDb(@PathVariable("tableName") String tableName)
    {
        genTableService.synchDb(tableName);
        return success();
    }

    /**
     * 批量生成多表代码——打包成一个 ZIP 下载。
     *
     * @param response HTTP 响应
     * @param tables   业务表名（逗号分隔字符串）
     * @throws IOException 写出异常
     */
    @PreAuthorize("@ss.hasPermi('tool:gen:code')")
    @Log(title = "代码生成", businessType = BusinessType.GENCODE)
    @GetMapping("/batchGenCode")
    public void batchGenCode(HttpServletResponse response, String tables) throws IOException
    {
        String[] tableNames = Convert.toStrArray(tables);
        byte[] data = genTableService.downloadCode(tableNames);
        genCode(response, data);
    }

    /**
     * 将 ZIP 字节流写入 HTTP 响应——设置 Content-Disposition 为 ruoyi.zip。
     *
     * @param response HTTP 响应
     * @param data     ZIP 字节数组
     * @throws IOException 写出异常
     */
    private void genCode(HttpServletResponse response, byte[] data) throws IOException
    {
        response.reset();
        response.addHeader("Access-Control-Allow-Origin", "*");
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition");
        response.setHeader("Content-Disposition", "attachment; filename=\"ruoyi.zip\"");
        response.addHeader("Content-Length", "" + data.length);
        response.setContentType("application/octet-stream; charset=UTF-8");
        IOUtils.write(data, response.getOutputStream());
    }
}