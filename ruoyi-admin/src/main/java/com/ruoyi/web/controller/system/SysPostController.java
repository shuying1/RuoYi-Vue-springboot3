package com.ruoyi.web.controller.system;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.poi.ExcelUtil;
import com.ruoyi.system.domain.SysPost;
import com.ruoyi.system.service.ISysPostService;

/**
 * 岗位管理接口控制器
 *
 * <p>业务模块：系统管理 — 岗位管理，路由前缀 {@code /system/post}，依赖 Service：{@link ISysPostService}。
 * 岗位是用户的次要组织维度：一个用户可以绑定多个岗位，用于报表统计、通知定向投递等场景。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/post/list}              — 分页查询岗位列表</li>
 *   <li>{@code POST   /system/post/export}            — 导出岗位 Excel</li>
 *   <li>{@code GET    /system/post/{postId}}          — 获取岗位详情</li>
 *   <li>{@code POST   /system/post}                   — 新增岗位（校验岗位名称、编码唯一性）</li>
 *   <li>{@code PUT    /system/post}                   — 修改岗位</li>
 *   <li>{@code DELETE /system/post/{postIds}}          — 批量删除岗位</li>
 *   <li>{@code GET    /system/post/optionselect}      — 岗位下拉选择器（用户管理分配岗位时使用）</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/post")
public class SysPostController extends BaseController
{
    /** 岗位 Service */
    @Autowired
    private ISysPostService postService;

    /**
     * 分页查询岗位列表
     *
     * <p>url: GET /system/post/list</p>
     *
     * @param url 请求路径
     * @param post 查询条件（postName、postCode、status 等）
     * @return TableDataInfo
     */
    @PreAuthorize("@ss.hasPermi('system:post:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysPost post)
    {
        startPage();
        List<SysPost> list = postService.selectPostList(post);
        return getDataTable(list);
    }

    /**
     * 导出岗位列表到 Excel
     *
     * <p>url: POST /system/post/export</p>
     *
     * @param url 请求路径
     * @param response HTTP 响应，用于写入 Excel 二进制流
     * @param post 导出筛选条件
     */
    @Log(title = "岗位管理", businessType = BusinessType.EXPORT)
    @PreAuthorize("@ss.hasPermi('system:post:export')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysPost post)
    {
        List<SysPost> list = postService.selectPostList(post);
        ExcelUtil<SysPost> util = new ExcelUtil<SysPost>(SysPost.class);
        util.exportExcel(response, list, "岗位数据");
    }

    /**
     * 根据岗位编号获取详细信息
     *
     * <p>url: GET /system/post/{postId}</p>
     *
     * @param url 请求路径
     * @param postId 岗位主键
     * @return AjaxResult#success(SysPost)
     */
    @PreAuthorize("@ss.hasPermi('system:post:query')")
    @GetMapping(value = "/{postId}")
    public AjaxResult getInfo(@PathVariable Long postId)
    {
        return success(postService.selectPostById(postId));
    }

    /**
     * 新增岗位
     *
     * <p>url: POST /system/post</p>
     *
     * <p>会同时校验岗位名称（postName）与岗位编码（postCode）的唯一性；
     * 自动将当前操作用户写入 createBy。</p>
     *
     * @param url 请求路径
     * @param post 岗位实体（经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:post:add')")
    @Log(title = "岗位管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysPost post)
    {
        if (!postService.checkPostNameUnique(post))
        {
            return error("新增岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        else if (!postService.checkPostCodeUnique(post))
        {
            return error("新增岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        post.setCreateBy(getUsername());
        return toAjax(postService.insertPost(post));
    }

    /**
     * 修改岗位
     *
     * <p>url: PUT /system/post</p>
     *
     * <p>同 {@link #add}，会校验名称与编码唯一性；自动写入 updateBy。</p>
     *
     * @param url 请求路径
     * @param post 岗位实体（含 postId，经 {@code @Validated} 校验）
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:post:edit')")
    @Log(title = "岗位管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysPost post)
    {
        if (!postService.checkPostNameUnique(post))
        {
            return error("修改岗位'" + post.getPostName() + "'失败，岗位名称已存在");
        }
        else if (!postService.checkPostCodeUnique(post))
        {
            return error("修改岗位'" + post.getPostName() + "'失败，岗位编码已存在");
        }
        post.setUpdateBy(getUsername());
        return toAjax(postService.updatePost(post));
    }

    /**
     * 批量删除岗位
     *
     * <p>url: DELETE /system/post/{postIds}</p>
     *
     * @param url 请求路径
     * @param postIds 需要删除的岗位主键数组
     * @return AjaxResult
     */
    @PreAuthorize("@ss.hasPermi('system:post:remove')")
    @Log(title = "岗位管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{postIds}")
    public AjaxResult remove(@PathVariable Long[] postIds)
    {
        return toAjax(postService.deletePostByIds(postIds));
    }

    /**
     * 获取岗位选择框列表
     *
     * <p>url: GET /system/post/optionselect</p>
     *
     * <p>用于用户管理中的"分配岗位"下拉选择。</p>
     *
     * @param url 请求路径
     * @return AjaxResult#success(List<SysPost>)
     */
    @GetMapping("/optionselect")
    public AjaxResult optionselect()
    {
        List<SysPost> posts = postService.selectPostAll();
        return success(posts);
    }
}
