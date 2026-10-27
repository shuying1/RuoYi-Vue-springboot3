package com.ruoyi.common.constant;

/**
 * HTTP响应状态码常量类
 * 
 * 定义系统API响应中使用的标准HTTP状态码和自定义业务状态码。
 * 遵循RFC 7231标准，同时扩展了6xx段用于自定义警告状态。
 *
 * 主要在 {@link com.ruoyi.common.core.domain.AjaxResult} 和
 * {@link com.ruoyi.common.core.domain.R} 等响应封装类中引用。
 * 
 * @author ruoyi
 */
public class HttpStatus
{
    /**
     * 操作成功状态码 200 OK
     */
    public static final int SUCCESS = 200;

    /**
     * 对象创建成功状态码 201 Created
     */
    public static final int CREATED = 201;

    /**
     * 请求已被接受但处理尚未完成状态码 202 Accepted
     */
    public static final int ACCEPTED = 202;

    /**
     * 操作执行成功但无返回内容状态码 204 No Content
     */
    public static final int NO_CONTENT = 204;

    /**
     * 资源已被永久移除，应使用新URL状态码 301 Moved Permanently
     */
    public static final int MOVED_PERM = 301;

    /**
     * 重定向，客户端应继续请求新地址状态码 303 See Other
     */
    public static final int SEE_OTHER = 303;

    /**
     * 资源未被修改，可使用缓存版本状态码 304 Not Modified
     */
    public static final int NOT_MODIFIED = 304;

    /**
     * 请求参数错误（缺少或格式不匹配）状态码 400 Bad Request
     */
    public static final int BAD_REQUEST = 400;

    /**
     * 未授权，需要认证状态码 401 Unauthorized
     */
    public static final int UNAUTHORIZED = 401;

    /**
     * 访问受限，授权过期或权限不足状态码 403 Forbidden
     */
    public static final int FORBIDDEN = 403;

    /**
     * 请求的资源或服务未找到状态码 404 Not Found
     */
    public static final int NOT_FOUND = 404;

    /**
     * 不允许的HTTP请求方法状态码 405 Method Not Allowed
     */
    public static final int BAD_METHOD = 405;

    /**
     * 资源冲突或资源被锁定状态码 409 Conflict
     */
    public static final int CONFLICT = 409;

    /**
     * 不支持的媒体类型或数据格式状态码 415 Unsupported Media Type
     */
    public static final int UNSUPPORTED_TYPE = 415;

    /**
     * 系统内部错误状态码 500 Internal Server Error
     */
    public static final int ERROR = 500;

    /**
     * 接口未实现状态码 501 Not Implemented
     */
    public static final int NOT_IMPLEMENTED = 501;

    /**
     * 自定义业务警告状态码 601 WARN
     * 用于业务层面的非致命性提示，如"部分数据处理成功"等场景
     */
    public static final int WARN = 601;
}
