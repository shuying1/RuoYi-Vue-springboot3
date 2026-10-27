package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.SysNotice;

/**
 * 通知公告 业务接口
 *
 * 提供通知公告的 CRUD、批量删除等能力。
 * 支持两种公告类型：通知（noticeType=1）和公告（noticeType=2）；状态分为正常（status=0，前端可见）和关闭（status=1）。
 * 公告的已读/未读状态管理由独立的 ISysNoticeReadService 负责，本接口专注于公告本身的管理。
 * 删除公告时级联清理已读记录由 Service 层调用 SysNoticeReadMapper.deleteByNoticeIds 完成。
 * 依赖：SysNoticeMapper。
 *
 * @author ruoyi
 */
public interface ISysNoticeService
{
    /**
     * 查询公告信息
     * 
     * @param noticeId 公告ID
     * @return 公告信息
     */
    public SysNotice selectNoticeById(Long noticeId);

    /**
     * 查询公告列表
     * 
     * @param notice 公告信息
     * @return 公告集合
     */
    public List<SysNotice> selectNoticeList(SysNotice notice);

    /**
     * 新增公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    public int insertNotice(SysNotice notice);

    /**
     * 修改公告
     * 
     * @param notice 公告信息
     * @return 结果
     */
    public int updateNotice(SysNotice notice);

    /**
     * 删除公告信息
     * 
     * @param noticeId 公告ID
     * @return 结果
     */
    public int deleteNoticeById(Long noticeId);

    /**
     * 批量删除公告信息
     * 
     * @param noticeIds 需要删除的公告ID
     * @return 结果
     */
    public int deleteNoticeByIds(Long[] noticeIds);
}
