package com.ruoyi.common.core.page;

import com.ruoyi.common.utils.StringUtils;

/**
 * 分页请求数据对象
 * 
 * 封装从前端请求参数中解析出的分页和排序参数，由 {@link TableSupport} 从
 * HttpServletRequest 中提取并构建。传递给 PageHelper 作为分页和排序配置。
 * 
 * @author ruoyi
 */
public class PageDomain
{
    /**
     * 当前请求的页码（从1开始）
     */
    private Integer pageNum;

    /**
     * 每页显示的记录条数
     */
    private Integer pageSize;

    /**
     * 排序字段名（驼峰命名，如 "createTime"），会被转换为下划线命名 "create_time"
     */
    private String orderByColumn;

    /**
     * 排序方向，值为 "asc" 或 "desc"，也兼容前端的 "ascending"/"descending"
     */
    private String isAsc = "asc";

    /**
     * 分页参数合理化开关，开启后 PageHelper会自动修正越界的pageNum
     */
    private Boolean reasonable = true;

    /**
     * 构建完整的ORDER BY子句，用于传给PageHelper.orderBy()
     * 将orderByColumn转为下划线命名后拼接排序方向
     *
     * @return ORDER BY子句字符串，如 "create_time desc"；无排序配置时返回空串
     */
    public String getOrderBy()
    {
        if (StringUtils.isEmpty(orderByColumn))
        {
            return "";
        }
        return StringUtils.toUnderScoreCase(orderByColumn) + " " + isAsc;
    }

    /**
     * 获取当前页码
     * @return 页码
     */
    public Integer getPageNum()
    {
        return pageNum;
    }

    /**
     * 设置当前页码
     * @param pageNum 页码，从1开始
     */
    public void setPageNum(Integer pageNum)
    {
        this.pageNum = pageNum;
    }

    /**
     * 获取每页记录数
     * @return 每页条数
     */
    public Integer getPageSize()
    {
        return pageSize;
    }

    /**
     * 设置每页记录数
     * @param pageSize 每页条数
     */
    public void setPageSize(Integer pageSize)
    {
        this.pageSize = pageSize;
    }

    /**
     * 获取排序字段（驼峰命名）
     * @return 排序字段名
     */
    public String getOrderByColumn()
    {
        return orderByColumn;
    }

    /**
     * 设置排序字段（驼峰命名）
     * @param orderByColumn 排序字段
     */
    public void setOrderByColumn(String orderByColumn)
    {
        this.orderByColumn = orderByColumn;
    }

    /**
     * 获取排序方向
     * @return "asc" 或 "desc"
     */
    public String getIsAsc()
    {
        return isAsc;
    }

    /**
     * 设置排序方向，兼容前端的 ascending/descending 写法，
     * 会自动转换为标准的 asc/desc
     * 
     * @param isAsc 排序方向
     */
    public void setIsAsc(String isAsc)
    {
        if (StringUtils.isNotEmpty(isAsc))
        {
            // 兼容前端不同的排序类型写法
            if ("ascending".equals(isAsc))
            {
                isAsc = "asc";
            }
            else if ("descending".equals(isAsc))
            {
                isAsc = "desc";
            }
            this.isAsc = isAsc;
        }
    }

    /**
     * 获取分页合理化设置
     * @return true=自动修正越界页码，默认true
     */
    public Boolean getReasonable()
    {
        if (StringUtils.isNull(reasonable))
        {
            return Boolean.TRUE;
        }
        return reasonable;
    }

    /**
     * 设置分页合理化开关
     * @param reasonable true=开启合理化
     */
    public void setReasonable(Boolean reasonable)
    {
        this.reasonable = reasonable;
    }
}
