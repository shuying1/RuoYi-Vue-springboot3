package com.ruoyi.common.utils.uuid;

/**
 * 各类 ID 生成工具类
 *
 * <p>提供随机 UUID、简化版 UUID（去除横线）、雪花算法（Snowflake）ID、
 * 基于时间戳的序列 ID 等多种唯一标识生成方法。
 *
 * <p>典型用法：主键生成、Token 关联 ID、临时文件名、分布式 ID。
 *
 * @author ruoyi
 */
public class IdUtils
{
    /**
     * 获取随机UUID
     * 
     * @return 随机UUID
     */
    public static String randomUUID()
    {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     * 
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID()
    {
        return UUID.randomUUID().toString(true);
    }

    /**
     * 获取随机UUID，使用性能更好的ThreadLocalRandom生成UUID
     * 
     * @return 随机UUID
     */
    public static String fastUUID()
    {
        return UUID.fastUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线，使用性能更好的ThreadLocalRandom生成UUID
     * 
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUUID()
    {
        return UUID.fastUUID().toString(true);
    }
}
