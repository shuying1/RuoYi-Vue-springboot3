package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.common.core.domain.entity.SysDictType;

/**
 * 字典类型表 sys_dict_type 数据访问层
 *
 * 提供字典类型的 CRUD、按类型值查询、唯一性校验查询等操作。
 * 字典类型是字典数据的分类容器（如"用户性别"、"系统开关"），
 * 具体下拉选项存储在 sys_dict_data 表中，通过 dict_type 关联。
 * Service 层会在 @PostConstruct 时将所有启用状态的字典数据加载到 DictUtils 缓存。
 * SQL 映射文件位于 resources/mapper/system/SysDictTypeMapper.xml。
 *
 * @author ruoyi
 */
public interface SysDictTypeMapper
{
    /**
     * 根据条件分页查询字典类型
     * 
     * @param dictType 字典类型信息
     * @return 字典类型集合信息
     */
    public List<SysDictType> selectDictTypeList(SysDictType dictType);

    /**
     * 根据所有字典类型
     * 
     * @return 字典类型集合信息
     */
    public List<SysDictType> selectDictTypeAll();

    /**
     * 根据字典类型ID查询信息
     * 
     * @param dictId 字典类型ID
     * @return 字典类型
     */
    public SysDictType selectDictTypeById(Long dictId);

    /**
     * 根据字典类型查询信息
     * 
     * @param dictType 字典类型
     * @return 字典类型
     */
    public SysDictType selectDictTypeByType(String dictType);

    /**
     * 通过字典ID删除字典信息
     * 
     * @param dictId 字典ID
     * @return 结果
     */
    public int deleteDictTypeById(Long dictId);

    /**
     * 批量删除字典类型信息
     * 
     * @param dictIds 需要删除的字典ID
     * @return 结果
     */
    public int deleteDictTypeByIds(Long[] dictIds);

    /**
     * 新增字典类型信息
     * 
     * @param dictType 字典类型信息
     * @return 结果
     */
    public int insertDictType(SysDictType dictType);

    /**
     * 修改字典类型信息
     * 
     * @param dictType 字典类型信息
     * @return 结果
     */
    public int updateDictType(SysDictType dictType);

    /**
     * 校验字典类型称是否唯一
     * 
     * @param dictType 字典类型
     * @return 结果
     */
    public SysDictType checkDictTypeUnique(String dictType);
}
