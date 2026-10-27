package com.ruoyi.common.core.page;

import java.io.Serializable;
import java.util.List;

/**
 * 表格分页响应数据封装类
 * 
 * 用于返回分页查询的结果集，包含总记录数、当前页数据行、响应状态码和消息。
 * 是 {@link com.ruoyi.common.core.controller.BaseController#getDataTable(List)} 方法
 * 的返回类型，直接序列化为前端表格组件所需的格式。
 *
 * JSON格式：{code: 200, msg: "查询成功", total: 100, rows: [...]}
 * 
 * @author ruoyi
 */
public class TableDataInfo implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * 符合查询条件的总记录数（不是当前页条数）
     */
    private long total;

    /**
     * 当前页的数据行列表
     */
    private List<?> rows;

    /**
     * 响应状态码，200表示成功
     */
    private int code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 默认构造函数
     */
    public TableDataInfo()
    {
    }

    /**
     * 分页数据构造函数
     * 
     * @param list  当前页的数据行列表
     * @param total 查询条件匹配的总记录数
     */
    public TableDataInfo(List<?> list, long total)
    {
        this.rows = list;
        this.total = total;
    }

    /**
     * 获取总记录数
     * @return 数据库中符合条件的总行数
     */
    public long getTotal()
    {
        return total;
    }

    /**
     * 设置总记录数
     * @param total 总记录数
     */
    public void setTotal(long total)
    {
        this.total = total;
    }

    /**
     * 获取当前页数据行
     * @return 数据行列表
     */
    public List<?> getRows()
    {
        return rows;
    }

    /**
     * 设置当前页数据行
     * @param rows 数据行列表
     */
    public void setRows(List<?> rows)
    {
        this.rows = rows;
    }

    /**
     * 获取响应状态码
     * @return 200=成功
     */
    public int getCode()
    {
        return code;
    }

    /**
     * 设置响应状态码
     * @param code HTTP状态码
     */
    public void setCode(int code)
    {
        this.code = code;
    }

    /**
     * 获取响应消息
     * @return 提示消息
     */
    public String getMsg()
    {
        return msg;
    }

    /**
     * 设置响应消息
     * @param msg 提示消息
     */
    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}
