package com.ruoyi.system.domain;

import java.util.Date;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 公告已读记录表 sys_notice_read
 *
 * 公告-用户已读关联实体类，对应数据库表 sys_notice_read。
 * 用于记录每个用户对每条公告的阅读状态，实现公告的"已读/未读"标记功能。
 * 该表与 sys_notice 表形成多对多关系的一方，通过 noticeId 和 userId 联合唯一。
 * 在 SysNoticeServiceImpl 中标记已读、查询未读数量、联表查询带已读状态的公告列表等场景使用。
 *
 * @author ruoyi
 */
public class SysNoticeRead
{
    /** 已读记录主键，数据库自增 */
    private Long readId;

    /** 公告ID，关联 sys_notice.notice_id；与 userId 联合唯一；不可空 */
    private Long noticeId;

    /** 用户ID，关联 sys_user.user_id；与 noticeId 联合唯一；不可空 */
    private Long userId;

    /** 阅读时间，由 SysNoticeReadServiceImpl.markRead 写入；不可空 */
    private Date readTime;

    /**
     * 获取主键ID
     *
     * @return 已读记录主键
     */
    public Long getReadId()
    {
        return readId;
    }

    /**
     * 设置主键ID
     *
     * @param readId 主键
     */
    public void setReadId(Long readId)
    {
        this.readId = readId;
    }

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
     * 获取用户ID
     *
     * @return 用户主键
     */
    public Long getUserId()
    {
        return userId;
    }

    /**
     * 设置用户ID
     *
     * @param userId 用户主键
     */
    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    /**
     * 获取阅读时间
     *
     * @return 已读发生的时间点
     */
    public Date getReadTime()
    {
        return readTime;
    }

    /**
     * 设置阅读时间
     *
     * @param readTime 已读时间
     */
    public void setReadTime(Date readTime)
    {
        this.readTime = readTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("readId", getReadId())
            .append("noticeId", getNoticeId())
            .append("userId", getUserId())
            .append("readTime", getReadTime())
            .toString();
    }
}
