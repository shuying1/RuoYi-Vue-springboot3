package com.ruoyi.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.SysNotice;
import com.ruoyi.system.service.ISysNoticeReadService;
import com.ruoyi.system.service.ISysNoticeService;

/**
 * 通知公告接口控制器
 *
 * <p>业务模块：系统管理 — 通知公告，路由前缀 {@code /system/notice}，
 * 依赖 Service：{@link ISysNoticeService}（公告主体 CRUD）、
 * {@link ISysNoticeReadService}（用户已读状态管理）。
 * 公告支持后台管理发布和首页顶栏展示，已读状态记录在 {@code sys_notice_read} 表中。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/notice/list}              — 分页查询公告列表（管理端）</li>
 *   <li>{@code GET    /system/notice/{noticeId}}        — 获取公告详情</li>
 *   <li>{@code POST   /system/notice}                   — 新增公告</li>
 *   <li>{@code PUT    /system/notice}                   — 修改公告</li>
 *   <li>{@code GET    /system/notice/listTop}           — 首页顶部公告（最多 5 条，含已读标记和未读数）</li>
 *   <li>{@code POST   /system/notice/markRead}          — 标记单条公告已读</li>
 *   <li>{@code POST   /system/notice/markReadAll}       — 批量标记已读</li>
 *   <li>{@code GET    /system/notice/readUsers/list}    — 查询公告已读用户列表</li>
 *   <li>{@code DELETE /system/notice/{noticeIds}}        — 批量删除公告</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/notice")
public class SysNoticeController extends BaseController
{
    /** 公告 Service */
    @Autowired
    private ISysNoticeService noticeService;

    /** 已读状态 Service：负责记录公告的用户已读情况 */
    @Autowired
    private ISysNoticeReadService noticeReadService;

    /**
     * 分页查询通知公告列表（管理端）
     *
     * <p>url: GET /system/notice/list</p>
     *
     * @param url 请求路径
     * @param notice 查询条件（noticeTitle、noticeType、createBy 等）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysNotice notice)
    {
        startPage();
        List<SysNotice> list = noticeService.selectNoticeList(notice);
        return getDataTable(list);
    }

    /**
     * 根据通知公告编号获取详细信息
     *
     * <p>url: GET /system/notice/{noticeId}</p>
     *
     * @param url 请求路径
     * @param noticeId 公告主键
     * @return AjaxResult#success(SysNotice)
     */
    @GetMapping(value = "/{noticeId}")
    public AjaxResult getInfo(@PathVariable Long noticeId)
    {
        return success(noticeService.selectNoticeById(noticeId));
    }

    /**
     * 新增通知公告
     *
     * <p>url: POST /system/notice</p>
     *
     * <p>自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param notice 公告实体（经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:notice:add')")
    @Log(title = "通知公告", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysNotice notice)
    {
        notice.setCreateBy(getUsername());
        return toAjax(noticeService.insertNotice(notice));
    }

    /**
     * 修改通知公告
     *
     * <p>url: PUT /system/notice</p>
     *
     * <p>自动将当前操作用户写入 updateBy。</p>
     *
     * @param url 请求路径
     * @param notice 公告实体（含 noticeId，经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:notice:edit')")
    @Log(title = "通知公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysNotice notice)
    {
        notice.setUpdateBy(getUsername());
        return toAjax(noticeService.updateNotice(notice));
    }

    /**
     * 首页顶部公告列表（公开接口，无需权限）
     *
     * <p>url: GET /system/notice/listTop</p>
     *
     * <p>返回当前用户最多 5 条的已发布公告，每条带 {@code isRead} 标记，
     * 并在响应体中附带 {@code unreadCount}（未读公告总数）。</p>
     *
     * @param url 请求路径
     * @return AjaxResult 含公告列表与 unreadCount
     */
    @GetMapping("/listTop")
    @ResponseBody
    public AjaxResult listTop()
    {
        Long userId = getUserId();
        List<SysNotice> list = noticeReadService.selectNoticeListWithReadStatus(userId, 5);
        long unreadCount = list.stream().filter(n -> !n.getIsRead()).count();
        AjaxResult result = AjaxResult.success(list);
        result.put("unreadCount", unreadCount);
        return result;
    }

    /**
     * 标记单条公告为已读
     *
     * <p>url: POST /system/notice/markRead</p>
     *
     * @param url 请求路径
     * @param noticeId 公告主键
     * @return AjaxResult#success()
     */
    @PostMapping("/markRead")
    @ResponseBody
    public AjaxResult markRead(Long noticeId)
    {
        Long userId = getUserId();
        noticeReadService.markRead(noticeId, userId);
        return success();
    }

    /**
     * 批量标记公告为已读
     *
     * <p>url: POST /system/notice/markReadAll</p>
     *
     * <p>参数 ids 为逗号分隔的公告 ID 字符串。</p>
     *
     * @param url 请求路径
     * @param ids 公告主键字符串（逗号分隔）
     * @return AjaxResult#success()
     */
    @PostMapping("/markReadAll")
    @ResponseBody
    public AjaxResult markReadAll(String ids)
    {
        Long userId = getUserId();
        Long[] noticeIds = Convert.toLongArray(ids);
        noticeReadService.markReadBatch(userId, noticeIds);
        return success();
    }

    /**
     * 查询公告已读用户列表（管理端）
     *
     * <p>url: GET /system/notice/readUsers/list</p>
     *
     * @param url 请求路径
     * @param noticeId 公告主键
     * @param searchValue 可选，用户名模糊搜索
     * @return TableDataInfo 已读用户分页列表
     */
    @PreAuthorize("@ss.hasPermi('system:notice:list')")
    @GetMapping("/readUsers/list")
    @ResponseBody
    public TableDataInfo readUsersList(Long noticeId, String searchValue)
    {
        startPage();
        List<?> list = noticeReadService.selectReadUsersByNoticeId(noticeId, searchValue);
        return getDataTable(list);
    }

    /**
     * 批量删除通知公告
     *
     * <p>url: DELETE /system/notice/{noticeIds}</p>
     *
     * <p>删除公告时会同时清理 sys_notice_read 中的关联已读记录。</p>
     *
     * @param url 请求路径
     * @param noticeIds 需要删除的公告主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:notice:remove')")
    @Log(title = "通知公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{noticeIds}")
    public AjaxResult remove(@PathVariable Long[] noticeIds)
    {
        noticeReadService.deleteByNoticeIds(noticeIds);
        return toAjax(noticeService.deleteNoticeByIds(noticeIds));
    }
}
