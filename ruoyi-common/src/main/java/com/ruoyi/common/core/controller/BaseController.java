package com.ruoyi.common.core.controller;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.PageDomain;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.page.TableSupport;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.PageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.sql.SqlUtil;

/**
 * 通用Web层控制器基类
 * 
 * 所有Controller都应继承此类，提供以下通用能力：
 * 1. 自动将前端传入的日期字符串转换为Date类型（InitBinder）
 * 2. 便捷的分页支持（startPage/startOrderBy/clearPage）
 * 3. 统一的响应封装（success/error/warn/toAjax）
 * 4. 当前登录用户快捷获取（getLoginUser/getUserId/getDeptId/getUsername）
 * 
 * @author ruoyi
 */
public class BaseController
{
    /**
     * 当前类的SLF4J日志记录器
     */
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 初始化数据绑定器
     * 注册自定义PropertyEditor，将前端传入的日期格式字符串自动转换为Date对象。
     * 支持的格式由 {@link DateUtils#parseDate(String)} 支持的模式决定。
     *
     * @param binder Spring MVC数据绑定器
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // 注册Date类型转换编辑器，将字符串日期自动解析为Date对象
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                setValue(DateUtils.parseDate(text));
            }
        });
    }

    /**
     * 设置请求分页数据
     * 从HTTP请求参数中解析pageNum和pageSize，调用PageHelper.startPage()建立分页上下文
     */
    protected void startPage()
    {
        PageUtils.startPage();
    }

    /**
     * 设置请求排序数据
     * 从HTTP请求参数中解析orderByColumn和isAsc，转义后调用PageHelper.orderBy()
     */
    protected void startOrderBy()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        if (StringUtils.isNotEmpty(pageDomain.getOrderBy()))
        {
            // 排序字段和方向需要转义防止SQL注入
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            PageHelper.orderBy(orderBy);
        }
    }

    /**
     * 清理PageHelper的ThreadLocal分页上下文
     * 建议在分页查询结束后调用，防止ThreadLocal内存泄漏
     */
    protected void clearPage()
    {
        PageUtils.clearPage();
    }

    /**
     * 将查询到的列表数据封装为分页响应
     * 自动计算总记录数（通过PageInfo.getTotal）并返回TableDataInfo
     *
     * @param list MyBatis查询结果列表（已在startPage后自动分页）
     * @return 包含rows和total的分页响应对象
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TableDataInfo getDataTable(List<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        // PageHelper拦截后的List可通过PageInfo获取真实总数
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 返回成功响应，无返回数据
     * 
     * @return code=200的AjaxResult
     */
    public AjaxResult success()
    {
        return AjaxResult.success();
    }

    /**
     * 返回失败响应
     * 
     * @return code=500的AjaxResult
     */
    public AjaxResult error()
    {
        return AjaxResult.error();
    }

    /**
     * 返回带消息的成功响应
     * 
     * @param message 成功提示消息
     * @return AjaxResult
     */
    public AjaxResult success(String message)
    {
        return AjaxResult.success(message);
    }
    
    /**
     * 返回带数据的成功响应
     * 
     * @param data 需要返回给前端的数据
     * @return AjaxResult
     */
    public AjaxResult success(Object data)
    {
        return AjaxResult.success(data);
    }

    /**
     * 返回带消息的失败响应
     * 
     * @param message 失败提示消息
     * @return AjaxResult
     */
    public AjaxResult error(String message)
    {
        return AjaxResult.error(message);
    }

    /**
     * 返回警告响应（状态码601）
     * 
     * @param message 警告提示消息
     * @return AjaxResult
     */
    public AjaxResult warn(String message)
    {
        return AjaxResult.warn(message);
    }

    /**
     * 根据影响行数转换为响应结果
     * 
     * @param rows 数据库受影响行数，大于0表示操作成功
     * @return rows>0返回成功，否则返回失败
     */
    protected AjaxResult toAjax(int rows)
    {
        return rows > 0 ? AjaxResult.success() : AjaxResult.error();
    }

    /**
     * 根据布尔结果转换为响应结果
     * 
     * @param result 操作结果布尔值
     * @return true返回成功，false返回失败
     */
    protected AjaxResult toAjax(boolean result)
    {
        return result ? success() : error();
    }

    /**
     * 页面跳转，重定向到指定URL
     * 
     * @param url 目标跳转地址
     * @return Spring MVC重定向视图名称
     */
    public String redirect(String url)
    {
        return StringUtils.format("redirect:{}", url);
    }

    /**
     * 获取当前登录用户信息
     * 从Redis缓存中读取登录用户的完整信息
     * 
     * @return 当前登录用户对象
     */
    public LoginUser getLoginUser()
    {
        return SecurityUtils.getLoginUser();
    }

    /**
     * 获取当前登录用户ID
     * 
     * @return 用户ID
     */
    public Long getUserId()
    {
        return getLoginUser().getUserId();
    }

    /**
     * 获取当前登录用户所属部门ID
     * 
     * @return 部门ID
     */
    public Long getDeptId()
    {
        return getLoginUser().getDeptId();
    }

    /**
     * 获取当前登录用户名
     * 
     * @return 登录用户名
     */
    public String getUsername()
    {
        return getLoginUser().getUsername();
    }
}
