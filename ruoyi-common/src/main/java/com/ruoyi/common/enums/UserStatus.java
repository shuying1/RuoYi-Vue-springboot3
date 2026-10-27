package com.ruoyi.common.enums;

/**
 * 用户状态枚举
 * 
 * 定义用户账号的三种状态及对应编码，用于用户表status字段取值。
 * 
 * @author ruoyi
 */
public enum UserStatus
{
    /**
     * 正常启用状态，编码 "0"
     */
    OK("0", "正常"),

    /**
     * 停用状态（被冻结或禁用），编码 "1"
     */
    DISABLE("1", "停用"),

    /**
     * 已删除（逻辑删除），编码 "2"
     */
    DELETED("2", "删除");

    /**
     * 状态编码（数据库存储值）
     */
    private final String code;

    /**
     * 状态中文描述
     */
    private final String info;

    /**
     * 构造函数
     * 
     * @param code 状态编码
     * @param info 状态描述
     */
    UserStatus(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    /**
     * 获取状态编码
     * @return 数据库存储的状态值
     */
    public String getCode()
    {
        return code;
    }

    /**
     * 获取状态描述
     * @return 中文描述
     */
    public String getInfo()
    {
        return info;
    }
}
