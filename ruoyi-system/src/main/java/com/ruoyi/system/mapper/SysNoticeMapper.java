package com.ruoyi.system.mapper;

import java.util.List;
import com.ruoyi.system.domain.SysNotice;

/**
 * 通知公告表 sys_notice 数据访问层
 *
 * 提供通知公告的 CRUD 操作，不包含已读状态查询（已读状态由 SysNoticeReadMapper 负责）。
 * SQL 映射文件位于 resources/mapper/system/SysNoticeMapper.xml。
 *
 * @author ruoyi
 */
public interface SysNoticeMapper
{
    /**
     * 根据主键查询公告详情
     *
     * @param noticeId 公告主键ID
     * @return 公告记录，未匹配到返回 null
     */
    public SysNotice selectNoticeById(Long noticeId);

    /**
     * 条件分页查询公告列表
     * <p>支持按公告标题模糊查询、公告类型筛选、公告状态筛选</p>
     *
     * @param notice 公告查询条件
     * @return 符合条件的公告集合，无数据返回空列表
     */
    public List<SysNotice> selectNoticeList(SysNotice notice);

    /**
     * 新增公告
     *
     * @param notice 公告信息（需包含 noticeTitle、noticeType、noticeContent）
     * @return 受影响的行数，成功新增返回 1
     */
    public int insertNotice(SysNotice notice);

    /**
     * 修改公告
     *
     * @param notice 公告信息（需包含 noticeId 作为更新条件）
     * @return 受影响的行数，成功修改返回 1
     */
    public int updateNotice(SysNotice notice);

    /**
     * 删除单条公告
     * <p>删除公告时关联的 sys_notice_read 已读记录由 Service 层级联清理</p>
     *
     * @param noticeId 待删除的公告主键ID
     * @return 受影响的行数，成功删除返回 1
     */
    public int deleteNoticeById(Long noticeId);

    /**
     * 批量删除公告
     *
     * @param noticeIds 待删除的公告主键ID数组
     * @return 受影响的行数总和
     */
    public int deleteNoticeByIds(Long[] noticeIds);
}
