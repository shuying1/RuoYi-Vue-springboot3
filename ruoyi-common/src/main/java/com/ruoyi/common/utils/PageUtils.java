package com.ruoyi.common.utils;

import com.github.pagehelper.PageHelper;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.sql.SqlUtil;

/**
 * 分页工具类
 *
 * <p>继承 {@link com.github.pagehelper.PageHelper}，
 * 提供静态便捷方法 startPage()/startOrderBy()/clearPage()，
 * 直接从 {@link com.ruoyi.common.core.page.TableSupport} 中解析前端传入的分页和排序参数，
 * 设置 PageHelper 的 ThreadLocal 上下文，供后续 MyBatis 查询自动分页。
 *
 * <p>调用 startPage() 之后紧接着执行一次 MyBatis 查询即可自动触发分页拦截器；
 * clearPage() 在某些特殊场景（如批量操作、无查询但 ThreadLocal 已污染）需要手动清理。
 *
 * <p>技术栈：PageHelper
 *
 * @author ruoyi
 */
public class PageUtils extends PageHelper
{
    /**
     * 设置请求分页数据
     */
    public static void startPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
        Boolean reasonable = pageDomain.getReasonable();
        PageHelper.startPage(pageNum, pageSize, orderBy).setReasonable(reasonable);
    }

    /**
     * 清理分页的线程变量
     */
    public static void clearPage()
    {
        PageHelper.clearPage();
    }
}
