package com.ruoyi.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.xss.Xss;

/**
 * 通知公告表 sys_notice
 *
 * 通知公告实体类，对应数据库表 sys_notice。
 * 用于存储系统通知和公告信息，支持两种类型：通知（noticeType=1）和公告（noticeType=2）。
 * 公告状态分为正常（status=0，前端可见）和关闭（status=1，前端不可见）。
 * isRead 字段为扩展字段，表示当前登录用户是否已阅读该公告，不持久化到数据库，
 * 由业务层根据 sys_notice_read 关联表动态填充（通过 SysNoticeReadMapper.selectNoticeListWithReadStatus 实现）。
 *
 * @author ruoyi
 */
public class SysNotice extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 公告主键ID，数据库自增 */
    private Long noticeId;

    /** 公告标题，XSS 过滤，最大50字符；不可空 */
    private String noticeTitle;

    /** 公告类型：1=通知，2=公告；不可空 */
    private String noticeType;

    /** 公告正文内容，支持 HTML 富文本；可空 */
    private String noticeContent;

    /** 公告状态：0=正常（前端可见），1=关闭（前端不可见）；不可空 */
    private String status;

    /** 是否已读：true=已阅读，false=未读；非持久化字段，业务层动态填充 */
    @JsonProperty("isRead")
    private boolean isRead;

    /**
     * 获取公告ID
     *
     * @return 公告主键
     */
    public Long getNoticeId()
    {
        return noticeId;
    }

    /**
     * 设置公告ID
     *
     * @param noticeId 公告主键
     */
    public void setNoticeId(Long noticeId)
    {
        this.noticeId = noticeId;
    }

    /**
     * 设置公告标题
     *
     * @param noticeTitle 公告标题，最大50字符
     */
    public void setNoticeTitle(String noticeTitle)
    {
        this.noticeTitle = noticeTitle;
    }

    /**
     * 获取公告标题
     *
     * @return 公告标题文本
     */
    @Xss(message = "公告标题不能包含脚本字符")
    @NotBlank(message = "公告标题不能为空")
    @Size(min = 0, max = 50, message = "公告标题不能超过50个字符")
    public String getNoticeTitle()
    {
        return noticeTitle;
    }

    /**
     * 设置公告类型
     *
     * @param noticeType 公告类型：1=通知，2=公告
     */
    public void setNoticeType(String noticeType)
    {
        this.noticeType = noticeType;
    }

    /**
     * 获取公告类型
     *
     * @return 公告类型：1=通知，2=公告
     */
    public String getNoticeType()
    {
        return noticeType;
    }

    /**
     * 设置公告内容
     *
     * @param noticeContent 公告正文（支持 HTML）
     */
    public void setNoticeContent(String noticeContent)
    {
        this.noticeContent = noticeContent;
    }

    /**
     * 获取公告内容
     *
     * @return 公告正文字符串
     */
    public String getNoticeContent()
    {
        return noticeContent;
    }

    /**
     * 设置公告状态
     *
     * @param status 公告状态：0=正常，1=关闭
     */
    public void setStatus(String status)
    {
        this.status = status;
    }

    /**
     * 获取公告状态
     *
     * @return 公告状态：0=正常，1=关闭
     */
    public String getStatus()
    {
        return status;
    }

    /**
     * 获取已读状态
     *
     * @return true=当前用户已读，false=未读
     */
    public boolean getIsRead()
    {
        return isRead;
    }

    /**
     * 设置已读状态
     *
     * @param isRead 是否已读
     */
    public void setIsRead(boolean isRead)
    {
        this.isRead = isRead;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("noticeId", getNoticeId())
            .append("noticeTitle", getNoticeTitle())
            .append("noticeType", getNoticeType())
            .append("noticeContent", getNoticeContent())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
