package com.ruoyi.common.core.page;

import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.ServletUtils;

/**
 * 表格分页参数工具类
 * 
 * 定义前端传分页和排序参数的标准HTTP参数名，并提供从HttpServletRequest
 * 中解析这些参数为 {@link PageDomain} 对象的方法。
 *
 * 前端参数约定：
 * - pageNum: 当前页码
 * - pageSize: 每页条数
 * - orderByColumn: 排序字段（驼峰命名）
 * - isAsc: 排序方向（asc/desc）
 * - reasonable: 分页合理化开关
 * 
 * @author ruoyi
 */
public class TableSupport
{
    /**
     * HTTP请求中表示当前页码的参数名，默认值"pageNum"
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * HTTP请求中表示每页条数的参数名，默认值"pageSize"
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * HTTP请求中表示排序字段的参数名
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * HTTP请求中表示排序方向的参数名，值为"desc"或"asc"
     */
    public static final String IS_ASC = "isAsc";

    /**
     * HTTP请求中表示分页合理化的参数名
     */
    public static final String REASONABLE = "reasonable";

    /**
     * 从当前HttpServletRequest中解析分页参数，构建PageDomain对象
     * 
     * @return 封装好分页和排序配置的PageDomain对象
     */
    public static PageDomain getPageDomain()
    {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1));
        pageDomain.setPageSize(Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10));
        pageDomain.setOrderByColumn(ServletUtils.getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(ServletUtils.getParameter(IS_ASC));
        pageDomain.setReasonable(ServletUtils.getParameterToBool(REASONABLE));
        return pageDomain;
    }

    /**
     * 构建分页请求对象，内部调用 {@link #getPageDomain()}
     * 
     * @return PageDomain对象
     */
    public static PageDomain buildPageRequest()
    {
        return getPageDomain();
    }
}
