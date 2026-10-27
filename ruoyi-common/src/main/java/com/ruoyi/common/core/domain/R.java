package com.ruoyi.common.core.domain;

import java.io.Serializable;

import com.ruoyi.common.constant.HttpStatus;

/**
 * 通用响应包装类（泛型版本）
 * 
 * 提供类型安全的API响应封装，使用泛型 {@code T} 指定data字段的类型，
 * 相比 {@link AjaxResult} 的HashMap实现更加类型安全，适用于新接口的响应封装。
 *
 * 响应格式：{code: 200, msg: "操作成功", data: T}
 *
 * @param <T> data字段的类型
 * @author ruoyi
 */
public class R<T> implements Serializable {
//    private static final long serialVersionUID = 1L;

    /**
     * 成功状态码值，复用 {@link HttpStatus#SUCCESS} (200)
     */
    public static final int SUCCESS = HttpStatus.SUCCESS;

    /**
     * 失败状态码值，复用 {@link HttpStatus#ERROR} (500)
     */
    public static final int FAIL = HttpStatus.ERROR;

    /**
     * 响应状态码，200表示成功，500表示失败
     */
    private int code;

    /**
     * 响应提示消息
     */
    private String msg;

    /**
     * 响应数据对象，泛型T类型
     */
    private T data;

    /**
     * 创建成功响应（无数据，默认消息"操作成功"）
     * 
     * @param <T> 泛型类型
     * @return code=200的成功R对象
     */
    public static <T> R<T> ok() {
        return restResult(null, SUCCESS, "操作成功");
    }

    /**
     * 创建带数据的成功响应，默认消息"操作成功"
     * 
     * @param data 成功时返回的数据
     * @param <T>  泛型类型
     * @return code=200、data=data的成功R对象
     */
    public static <T> R<T> ok(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    /**
     * 创建带数据和自定义消息的成功响应
     * 
     * @param data 成功时返回的数据
     * @param msg  自定义成功消息
     * @param <T>  泛型类型
     * @return 成功R对象
     */
    public static <T> R<T> ok(T data, String msg) {
        return restResult(data, SUCCESS, msg);
    }

    /**
     * 创建失败响应（无数据，默认消息"操作失败"）
     * 
     * @param <T> 泛型类型
     * @return code=500的失败R对象
     */
    public static <T> R<T> fail() {
        return restResult(null, FAIL, "操作失败");
    }

    /**
     * 创建带自定义消息的失败响应，无数据
     * 
     * @param msg 自定义失败消息
     * @param <T> 泛型类型
     * @return code=500的失败R对象
     */
    public static <T> R<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    /**
     * 创建带数据的失败响应，默认消息"操作失败"
     * 
     * @param data 附加数据
     * @param <T>  泛型类型
     * @return code=500的失败R对象
     */
    public static <T> R<T> fail(T data) {
        return restResult(data, FAIL, "操作失败");
    }

    /**
     * 创建带数据和自定义消息的失败响应
     * 
     * @param data 附加数据
     * @param msg  自定义失败消息
     * @param <T>  泛型类型
     * @return 失败R对象
     */
    public static <T> R<T> fail(T data, String msg) {
        return restResult(data, FAIL, msg);
    }

    /**
     * 创建自定义状态码的失败响应，无数据
     * 
     * @param code 自定义错误码
     * @param msg  错误消息
     * @param <T>  泛型类型
     * @return 自定义状态码的失败R对象
     */
    public static <T> R<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 内部方法，构建R对象实例
     * 
     * @param data 响应数据
     * @param code 响应状态码
     * @param msg  响应消息
     * @param <T>  泛型类型
     * @return 构建好的R对象
     */
    private static <T> R<T> restResult(T data, int code, String msg) {
        R<T> apiResult = new R<>();
        apiResult.setCode(code);
        apiResult.setData(data);
        apiResult.setMsg(msg);
        return apiResult;
    }

    /**
     * 获取响应状态码
     * @return 状态码，200表示成功
     */
    public int getCode() {
        return code;
    }

    /**
     * 设置响应状态码
     * @param code 状态码
     */
    public void setCode(int code) {
        this.code = code;
    }

    /**
     * 获取响应消息
     * @return 提示消息
     */
    public String getMsg() {
        return msg;
    }

    /**
     * 设置响应消息
     * @param msg 提示消息
     */
    public void setMsg(String msg) {
        this.msg = msg;
    }

    /**
     * 获取响应数据
     * @return 业务数据对象
     */
    public T getData() {
        return data;
    }

    /**
     * 设置响应数据
     * @param data 业务数据对象
     */
    public void setData(T data) {
        this.data = data;
    }

    /**
     * 判断结果是否为失败（code不等于SUCCESS）
     * 
     * @param ret 待判断的响应对象
     * @param <T> 数据泛型
     * @return true=失败，false=成功
     */
    public static <T> Boolean isError(R<T> ret) {
        return !isSuccess(ret);
    }

    /**
     * 判断结果是否为成功（code等于SUCCESS）
     * 
     * @param ret 待判断的响应对象
     * @param <T> 数据泛型
     * @return true=成功，false=失败
     */
    public static <T> Boolean isSuccess(R<T> ret) {
        return R.SUCCESS == ret.getCode();
    }
}
