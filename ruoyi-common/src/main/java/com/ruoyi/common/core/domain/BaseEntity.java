package com.ruoyi.common.core.domain;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Setter;

/**
 * 系统实体基类
 * 
 * 提供实体类通用字段的封装，所有业务实体类都应继承此类以复用标准审计字段
 * 和搜索辅助字段。使用Lombok的@Data注解自动生成getter/setter方法。
 *
 * 包含以下字段：
 * - searchValue: 搜索关键字（非序列化到前端）
 * - createBy/createTime: 创建者和创建时间
 * - updateBy/updateTime: 更新者和更新时间
 * - remark: 备注信息
 * - params: 额外请求参数（不序列化空值）
 * 
 * @author ruoyi
 */
@Data
public class BaseEntity implements Serializable
{
    @Serial
    private static final long serialVersionUID = 5003261155502016826L;

    /**
     * 搜索关键字，用于前端传入模糊搜索条件，不参与JSON序列化到响应
     */
    @JsonIgnore
    private String searchValue;

    /**
     * 创建者用户名，记录创建该记录的操作人
     */
    private String createBy;

    /**
     * 创建时间，格式为 yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新者用户名，记录最后一次修改该记录的操作人
     */
    private String updateBy;

    /**
     * 更新时间，格式为 yyyy-MM-dd HH:mm:ss
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /**
     * 备注说明信息
     */
    private String remark;

    /**
     * 额外请求参数Map，用于动态传递非固定参数，序列化时自动排除空Map
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params;

    /**
     * 获取请求参数Map，若为null则自动初始化一个空HashMap
     * 
     * @return 请求参数Map对象，永不为null
     */
    public Map<String, Object> getParams()
    {
        if (params == null)
        {
            params = new HashMap<>();
        }
        return params;
    }

}
