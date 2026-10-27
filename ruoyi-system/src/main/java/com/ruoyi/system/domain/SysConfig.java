package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 参数配置表 sys_config
 *
 * 系统参数配置实体类，对应数据库表 sys_config。
 * 用于存储系统全局配置参数（如用户初始密码、账号自助注册开关、验证码开关等），
 * 通过 configKey 唯一标识，configValue 存储具体值。
 * 参数配置分为系统内置（configType=Y，不可删除）和自定义（configType=N）两种类型。
 * 运行时参数值会缓存在 Redis 中以提升读取性能。
 *
 * @author ruoyi
 */
public class SysConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 参数主键 */
    @Excel(name = "参数主键", cellType = ColumnType.NUMERIC)
    private Long configId;

    /** 参数名称 */
    @Excel(name = "参数名称")
    private String configName;

    /** 参数键名 */
    @Excel(name = "参数键名")
    private String configKey;

    /** 参数键值 */
    @Excel(name = "参数键值")
    private String configValue;

    /** 系统内置（Y是 N否） */
    @Excel(name = "系统内置", readConverterExp = "Y=是,N=否")
    private String configType;

    /**
     * 获取参数主键
     *
     * @return 配置记录主键ID
     */
    public Long getConfigId()
    {
        return configId;
    }

    /**
     * 设置参数主键
     *
     * @param configId 配置记录主键ID
     */
    public void setConfigId(Long configId)
    {
        this.configId = configId;
    }

    /**
     * 获取参数名称
     * <p>用于界面展示的参数中文名</p>
     *
     * @return 参数名称，最大长度100字符
     */
    @NotBlank(message = "参数名称不能为空")
    @Size(min = 0, max = 100, message = "参数名称不能超过100个字符")
    public String getConfigName()
    {
        return configName;
    }

    /**
     * 设置参数名称
     *
     * @param configName 参数名称
     */
    public void setConfigName(String configName)
    {
        this.configName = configName;
    }

    /**
     * 获取参数键名
     * <p>作为参数的唯一标识符，用于代码中读取配置值</p>
     *
     * @return 参数键名，最大长度100字符
     */
    @NotBlank(message = "参数键名长度不能为空")
    @Size(min = 0, max = 100, message = "参数键名长度不能超过100个字符")
    public String getConfigKey()
    {
        return configKey;
    }

    /**
     * 设置参数键名
     *
     * @param configKey 参数键名
     */
    public void setConfigKey(String configKey)
    {
        this.configKey = configKey;
    }

    /**
     * 获取参数键值
     * <p>参数的实际配置值，以字符串形式存储</p>
     *
     * @return 参数键值，最大长度500字符
     */
    @NotBlank(message = "参数键值不能为空")
    @Size(min = 0, max = 500, message = "参数键值长度不能超过500个字符")
    public String getConfigValue()
    {
        return configValue;
    }

    /**
     * 设置参数键值
     *
     * @param configValue 参数键值
     */
    public void setConfigValue(String configValue)
    {
        this.configValue = configValue;
    }

    /**
     * 获取参数类型
     *
     * @return 参数类型：Y=系统内置（不可删除），N=自定义
     */
    public String getConfigType()
    {
        return configType;
    }

    /**
     * 设置参数类型
     *
     * @param configType 参数类型：Y=系统内置，N=自定义
     */
    public void setConfigType(String configType)
    {
        this.configType = configType;
    }

    /**
     * 返回参数配置的字符串表示
     * <p>使用 Apache ToStringBuilder 多行格式输出所有字段</p>
     *
     * @return 包含所有字段值的多行字符串
     */
    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("configId", getConfigId())
            .append("configName", getConfigName())
            .append("configKey", getConfigKey())
            .append("configValue", getConfigValue())
            .append("configType", getConfigType())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
