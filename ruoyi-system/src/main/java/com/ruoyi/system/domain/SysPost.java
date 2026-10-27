package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.annotation.Excel.ColumnType;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 岗位表 sys_post
 *
 * 岗位实体类，对应数据库表 sys_post。
 * 岗位是组织架构中的职务节点，与用户通过 sys_user_post 表形成多对多关联。
 * 每个岗位有唯一的岗位编码、岗位名称、排序号和状态（正常/停用）。
 * flag 字段为扩展标记，用于在用户管理界面中标识某个用户是否已关联该岗位，不持久化到数据库。
 *
 * @author ruoyi
 */
public class SysPost extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 岗位主键ID，数据库自增 */
    @Excel(name = "岗位序号", cellType = ColumnType.NUMERIC)
    private Long postId;

    /** 岗位编码，业务唯一标识，如"ceo"、"cto"、"it"；不可空，最大64字符 */
    @Excel(name = "岗位编码")
    private String postCode;

    /** 岗位名称，中文名，如"总裁"、"技术总监"；不可空，最大50字符 */
    @Excel(name = "岗位名称")
    private String postName;

    /** 岗位排序号，数字越小越靠前；不可空 */
    @Excel(name = "岗位排序")
    private Integer postSort;

    /** 岗位状态：0=正常（生效中），1=停用（不再分配）；不可空 */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 用户是否存在此岗位标识，默认 false；非持久化字段，用于用户编辑界面回显 */
    private boolean flag = false;

    /**
     * 获取岗位ID
     *
     * @return 岗位主键
     */
    public Long getPostId()
    {
        return postId;
    }

    /**
     * 设置岗位ID
     *
     * @param postId 岗位主键
     */
    public void setPostId(Long postId)
    {
        this.postId = postId;
    }

    /**
     * 获取岗位编码
     *
     * @return 岗位编码字符串
     */
    @NotBlank(message = "岗位编码不能为空")
    @Size(min = 0, max = 64, message = "岗位编码长度不能超过64个字符")
    public String getPostCode()
    {
        return postCode;
    }

    /**
     * 设置岗位编码
     *
     * @param postCode 岗位编码
     */
    public void setPostCode(String postCode)
    {
        this.postCode = postCode;
    }

    /**
     * 获取岗位名称
     *
     * @return 岗位名称
     */
    @NotBlank(message = "岗位名称不能为空")
    @Size(min = 0, max = 50, message = "岗位名称长度不能超过50个字符")
    public String getPostName()
    {
        return postName;
    }

    /**
     * 设置岗位名称
     *
     * @param postName 岗位名称
     */
    public void setPostName(String postName)
    {
        this.postName = postName;
    }

    /**
     * 获取岗位排序号
     *
     * @return 排序值（升序排列）
     */
    @NotNull(message = "显示顺序不能为空")
    public Integer getPostSort()
    {
        return postSort;
    }

    /**
     * 设置岗位排序号
     *
     * @param postSort 排序值
     */
    public void setPostSort(Integer postSort)
    {
        this.postSort = postSort;
    }

    /**
     * 获取岗位状态
     *
     * @return 0=正常，1=停用
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * 设置岗位状态
     *
     * @param status 状态编码
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * 获取用户关联标识（非持久化）
     *
     * @return true=当前用户已分配此岗位
     */
    public boolean isFlag()
    {
        return flag;
    }

    /**
     * 设置用户关联标识
     *
     * @param flag 是否已关联
     */
    public void setFlag(boolean flag)
    {
        this.flag = flag;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("postId", getPostId())
            .append("postCode", getPostCode())
            .append("postName", getPostName())
            .append("postSort", getPostSort())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
