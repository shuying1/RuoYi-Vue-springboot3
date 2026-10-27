package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysConfig;

/**
 * 参数配置表 sys_config 数据访问层
 *
 * 提供系统参数配置的 CRUD 操作及唯一性校验查询。
 * 所有 SQL 映射位于 resources/mapper/system/SysConfigMapper.xml。
 * 参数配置值在 Service 层会被缓存到 Redis（key: "sys_config:" + configKey），
 * 本 Mapper 专注于持久化层操作。
 *
 * @author ruoyi
 */
public interface SysConfigMapper
{
    /**
     * 根据条件查询参数配置
     * <p>支持多字段非空条件匹配，通常用于列表筛选</p>
     *
     * @param config 参数配置查询条件（configId、configKey 等字段非空时作为条件）
     * @return 匹配的参数配置记录，未匹配到返回 null
     */
    public SysConfig selectConfig(SysConfig config);

    /**
     * 通过主键查询参数配置
     *
     * @param configId 参数配置主键ID
     * @return 参数配置记录，未匹配到返回 null
     */
    public SysConfig selectConfigById(Long configId);

    /**
     * 条件分页查询参数配置列表
     * <p>支持按参数名称模糊查询、参数键名模糊查询、参数类型筛选</p>
     *
     * @param config 参数配置查询条件
     * @return 符合条件的参数配置集合，无数据返回空列表
     */
    public List<SysConfig> selectConfigList(SysConfig config);

    /**
     * 根据键名查询参数配置（用于唯一性校验）
     *
     * @param configKey 参数键名（唯一业务标识）
     * @return 匹配的参数配置记录，未匹配到返回 null
     */
    public SysConfig checkConfigKeyUnique(String configKey);

    /**
     * 新增参数配置记录
     * <p>新增时会自动填充 createBy、createTime 等审计字段</p>
     *
     * @param config 参数配置信息（需包含 configKey、configValue）
     * @return 受影响的行数，成功新增返回 1
     */
    public int insertConfig(SysConfig config);

    /**
     * 修改参数配置记录
     * <p>修改时会自动更新 updateBy、updateTime 等审计字段</p>
     *
     * @param config 参数配置信息（需包含 configId 作为更新条件）
     * @return 受影响的行数，成功修改返回 1
     */
    public int updateConfig(SysConfig config);

    /**
     * 删除单条参数配置
     *
     * @param configId 待删除的参数配置主键ID
     * @return 受影响的行数，成功删除返回 1
     */
    public int deleteConfigById(Long configId);

    /**
     * 批量删除参数配置
     *
     * @param configIds 待删除的参数配置主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteConfigByIds(Long[] configIds);
}
