package com.ruoyi.common.core.domain;

import java.util.HashMap;
import java.util.Objects;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.utils.StringUtils;

/**
 * Ajax异步操作结果封装类
 * 
 * 继承HashMap实现键值对存储，使用约定的键名 {@link #CODE_TAG}、{@link #MSG_TAG}、{@link #DATA_TAG}
 * 分别存储状态码、提示消息和数据对象。提供一组静态工厂方法（success/warn/error）快速创建
 * 不同状态的响应对象，支持链式put操作以扩展额外字段。
 *
 * 响应格式：{code: 200, msg: "操作成功", data: {...}, ...}
 *
 * @author ruoyi
 */
public class AjaxResult extends HashMap<String, Object>
{
    private static final long serialVersionUID = 1L;

    /**
     * 响应中状态码的键名 "code"，存储HTTP状态码或自定义业务码
     */
    public static final String CODE_TAG = "code";

    /**
     * 响应中提示消息的键名 "msg"，存储操作结果的描述文本
     */
    public static final String MSG_TAG = "msg";

    /**
     * 响应中数据对象的键名 "data"，存储业务返回数据
     */
    public static final String DATA_TAG = "data";

    /**
     * 构造一个空的AjaxResult对象
     */
    public AjaxResult()
    {
    }

    /**
     * 构造AjaxResult对象，设置状态码和消息
     * 
     * @param code 响应状态码，通常使用 {@link HttpStatus} 中的常量
     * @param msg  响应提示消息
     */
    public AjaxResult(int code, String msg)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
    }

    /**
     * 构造AjaxResult对象，设置状态码、消息和数据对象
     * 
     * @param code 响应状态码
     * @param msg  响应提示消息
     * @param data 响应数据对象，若为null则不存入data字段
     */
    public AjaxResult(int code, String msg, Object data)
    {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (StringUtils.isNotNull(data))
        {
            super.put(DATA_TAG, data);
        }
    }

    /**
     * 返回成功消息，无返回数据，默认消息为"操作成功"
     * 
     * @return 成功状态的AjaxResult（code=200）
     */
    public static AjaxResult success()
    {
        return AjaxResult.success("操作成功");
    }

    /**
     * 返回成功数据，默认消息为"操作成功"
     * 
     * @param data 需要返回给前端的数据对象
     * @return 成功状态的AjaxResult（code=200, data=data）
     */
    public static AjaxResult success(Object data)
    {
        return AjaxResult.success("操作成功", data);
    }

    /**
     * 返回成功消息，无返回数据
     * 
     * @param msg 自定义成功消息
     * @return 成功状态的AjaxResult
     */
    public static AjaxResult success(String msg)
    {
        return AjaxResult.success(msg, null);
    }

    /**
     * 返回成功消息和数据对象
     * 
     * @param msg  自定义成功消息
     * @param data 返回的数据对象
     * @return 成功状态的AjaxResult（code=200）
     */
    public static AjaxResult success(String msg, Object data)
    {
        return new AjaxResult(HttpStatus.SUCCESS, msg, data);
    }

    /**
     * 返回警告消息，无返回数据，使用业务警告状态码601
     *
     * @param msg 警告提示消息
     * @return 警告状态的AjaxResult（code=601）
     */
    public static AjaxResult warn(String msg)
    {
        return AjaxResult.warn(msg, null);
    }

    /**
     * 返回警告消息和数据对象，使用业务警告状态码601
     *
     * @param msg  警告提示消息
     * @param data 附加数据
     * @return 警告状态的AjaxResult（code=601）
     */
    public static AjaxResult warn(String msg, Object data)
    {
        return new AjaxResult(HttpStatus.WARN, msg, data);
    }

    /**
     * 返回错误消息，无返回数据，默认消息为"操作失败"
     * 
     * @return 错误状态的AjaxResult（code=500）
     */
    public static AjaxResult error()
    {
        return AjaxResult.error("操作失败");
    }

    /**
     * 返回错误消息，无返回数据
     * 
     * @param msg 自定义错误消息
     * @return 错误状态的AjaxResult（code=500）
     */
    public static AjaxResult error(String msg)
    {
        return AjaxResult.error(msg, null);
    }

    /**
     * 返回错误消息和数据对象
     * 
     * @param msg  错误提示消息
     * @param data 附加数据对象
     * @return 错误状态的AjaxResult（code=500）
     */
    public static AjaxResult error(String msg, Object data)
    {
        return new AjaxResult(HttpStatus.ERROR, msg, data);
    }

    /**
     * 返回指定状态码的错误消息
     * 
     * @param code 自定义错误状态码
     * @param msg  错误提示消息
     * @return 自定义错误状态码的AjaxResult
     */
    public static AjaxResult error(int code, String msg)
    {
        return new AjaxResult(code, msg, null);
    }

    /**
     * 判断当前结果是否为成功消息
     *
     * @return true=成功（code=200）, false=非成功
     */
    public boolean isSuccess()
    {
        return Objects.equals(HttpStatus.SUCCESS, this.get(CODE_TAG));
    }

    /**
     * 判断当前结果是否为警告消息
     *
     * @return true=警告（code=601）, false=非警告
     */
    public boolean isWarn()
    {
        return Objects.equals(HttpStatus.WARN, this.get(CODE_TAG));
    }

    /**
     * 判断当前结果是否为错误消息
     *
     * @return true=错误（code=500）, false=非错误
     */
    public boolean isError()
    {
        return Objects.equals(HttpStatus.ERROR, this.get(CODE_TAG));
    }

    /**
     * 重写HashMap的put方法，支持链式调用，方便扩展额外字段
     *
     * @param key   扩展字段的键
     * @param value 扩展字段的值
     * @return 当前AjaxResult对象，支持链式调用
     */
    @Override
    public AjaxResult put(String key, Object value)
    {
        super.put(key, value);
        return this;
    }
}
