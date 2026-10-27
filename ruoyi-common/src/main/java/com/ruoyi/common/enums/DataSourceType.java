package com.ruoyi.common.enums;

/**
 * 数据源类型枚举
 * 
 * 用于多数据源切换场景，标记 @DataSource 注解中指定的目标数据源。
 * 支持主库读写和从库只读的分离架构。
 * 
 * @author ruoyi
 */
public enum DataSourceType
{
    /**
     * 主库（Master），支持读写操作
     */
    MASTER,

    /**
     * 从库（Slave），通常为只读操作
     */
    SLAVE
}
