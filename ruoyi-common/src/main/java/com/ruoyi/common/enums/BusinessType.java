package com.ruoyi.common.enums;

/**
 * 业务操作类型枚举
 * 
 * 在 @Log 操作日志注解中使用，标识被记录的业务操作属于哪种类型，
 * 便于日志查询时按类型过滤。
 * 
 * @author ruoyi
 */
public enum BusinessType
{
    /**
     * 其他操作（默认）
     */
    OTHER,

    /**
     * 新增操作
     */
    INSERT,

    /**
     * 修改操作
     */
    UPDATE,

    /**
     * 删除操作
     */
    DELETE,

    /**
     * 授权操作（如分配角色权限）
     */
    GRANT,

    /**
     * 导出操作（如Excel导出）
     */
    EXPORT,

    /**
     * 导入操作（如Excel导入）
     */
    IMPORT,

    /**
     * 强制下线操作（如管理员强制用户下线）
     */
    FORCE,

    /**
     * 代码生成操作（如代码生成器）
     */
    GENCODE,
    
    /**
     * 清空数据操作
     */
    CLEAN,
}
