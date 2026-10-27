package com.ruoyi.system.domain;

import com.ruoyi.common.utils.StringUtils;

/**
 * Redis 缓存信息封装类
 *
 * 该类用于在 Redis 缓存管理界面中展示缓存相关信息，包括缓存名称、键名、值及备注说明。
 * 主要作为缓存监控和管理模块的数据传输对象（DTO），由 SysCacheController 返回给前端。
 * 与其他 domain 类不同，该类不对应数据库表，是纯内存数据的封装。
 *
 * @author ruoyi
 */
public class SysCache
{
    /** 缓存名称（即 Redis key 的前缀部分） */
    private String cacheName = "";

    /** 缓存键名（完整的 Redis key 或除去名称前缀后的部分） */
    private String cacheKey = "";

    /** 缓存内容（Redis key 对应的 value 字符串形式） */
    private String cacheValue = "";

    /** 备注（缓存的业务说明，如"用户信息缓存"等） */
    private String remark = "";

    /**
     * 无参构造函数
     * <p>构造一个所有字段为空字符串的 SysCache 实例</p>
     */
    public SysCache()
    {

    }

    /**
     * 构造缓存名称和备注的实例
     *
     * @param cacheName 缓存名称（Redis key 前缀，如 "sys_config:"）
     * @param remark    缓存的业务备注说明
     */
    public SysCache(String cacheName, String remark)
    {
        this.cacheName = cacheName;
        this.remark = remark;
    }

    /**
     * 构造完整缓存信息的实例
     * <p>该构造函数会自动处理 cacheName 和 cacheKey 的格式：</p>
     * <ul>
     *     <li>将 cacheName 中的冒号 ":" 去除，得到纯净的缓存名称</li>
     *     <li>从 cacheKey 中移除 cacheName 前缀，得到相对键名</li>
     * </ul>
     * <p>例如：cacheName="sys_config:"，cacheKey="sys_config:1"，处理后 cacheName="sys_config"，cacheKey="1"</p>
     *
     * @param cacheName  缓存名称（可能包含冒号分隔符，构造时会自动清理）
     * @param cacheKey   缓存键名（可能包含名称前缀，构造时会自动去除）
     * @param cacheValue 缓存内容值
     */
    public SysCache(String cacheName, String cacheKey, String cacheValue)
    {
        this.cacheName = StringUtils.replace(cacheName, ":", "");
        this.cacheKey = StringUtils.replace(cacheKey, cacheName, "");
        this.cacheValue = cacheValue;
    }

    /**
     * 获取缓存名称
     *
     * @return 缓存名称字符串，默认空字符串
     */
    public String getCacheName()
    {
        return cacheName;
    }

    /**
     * 设置缓存名称
     *
     * @param cacheName 缓存名称
     */
    public void setCacheName(String cacheName)
    {
        this.cacheName = cacheName;
    }

    /**
     * 获取缓存键名
     *
     * @return 缓存键名字符串，默认空字符串
     */
    public String getCacheKey()
    {
        return cacheKey;
    }

    /**
     * 设置缓存键名
     *
     * @param cacheKey 缓存键名
     */
    public void setCacheKey(String cacheKey)
    {
        this.cacheKey = cacheKey;
    }

    /**
     * 获取缓存内容值
     *
     * @return 缓存内容字符串，默认空字符串
     */
    public String getCacheValue()
    {
        return cacheValue;
    }

    /**
     * 设置缓存内容值
     *
     * @param cacheValue 缓存内容
     */
    public void setCacheValue(String cacheValue)
    {
        this.cacheValue = cacheValue;
    }

    /**
     * 获取备注说明
     *
     * @return 备注字符串，默认空字符串
     */
    public String getRemark()
    {
        return remark;
    }

    /**
     * 设置备注说明
     *
     * @param remark 备注内容
     */
    public void setRemark(String remark)
    {
        this.remark = remark;
    }
}
