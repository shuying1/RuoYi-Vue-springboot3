package com.ruoyi.framework.web.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.exception.DemoModeException;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.html.EscapeUtil;

/**
 * 全局异常处理器 —— 统一捕获并转换异常为 AjaxResult JSON 响应
 *
 * <p>设计意图：使用 Spring MVC 的 {@link RestControllerAdvice} 机制，
 * 在所有 Controller 抛出异常后统一拦截，转换为前端可消费的 {@link AjaxResult} JSON 格式。
 * 避免在每个 Controller 中重复 try-catch，同时保证错误响应格式一致。</p>
 *
 * <p>异常处理策略（优先级从高到低）：</p>
 * <ol>
 *   <li>{@link AccessDeniedException} —— Spring Security 权限校验失败 → HTTP 403</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} —— 请求方法不支持 → 返回异常消息</li>
 *   <li>{@link ServiceException} —— 业务逻辑异常（最常用，业务层主动抛出） → 携带错误码和消息</li>
 *   <li>{@link MissingPathVariableException} —— 路径变量缺失</li>
 *   <li>{@link MethodArgumentTypeMismatchException} —— 参数类型不匹配（如传 "abc" 给 Long 类型），
 *       会先用 {@link EscapeUtil} 转义用户输入防止 XSS</li>
 *   <li>{@link BindException} / {@link MethodArgumentNotValidException} —— Spring 校验异常（@Valid / @Validated），
 *       取第一个校验错误的 message 返回</li>
 *   <li>{@link DemoModeException} —— 演示模式禁止操作 → "演示模式，不允许操作"</li>
 *   <li>{@link RuntimeException} —— 运行时兜底（非 Spring 自带的）</li>
 *   <li>{@link Exception} —— 最终兜底，捕获所有未被前面 handler 匹配的异常</li>
 * </ol>
 *
 * <p>日志记录策略：
 * 所有 handler 都会通过 {@link Logger#error} 打印完整的异常堆栈和请求 URI，
 * 便于后端排查问题。ServiceException 通常也会被 log.error 记录，
 * 因为其内部可能已经做过部分处理。</p>
 *
 * @author ruoyi
 * @see RestControllerAdvice Spring MVC 全局异常处理注解
 * @see AjaxResult 统一响应封装类
 * @see ServiceException 业务异常基类（各业务层主动抛出）
 */
@RestControllerAdvice
public class GlobalExceptionHandler
{
    /** 本地异常日志记录器 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 处理 Spring Security 权限校验失败 —— 返回 403。
     *
     * <p>触发场景：用户已登录但没有访问指定接口的权限，
     * 如 @PreAuthorize("hasRole('admin')") 校验失败。</p>
     *
     * @param e AccessDeniedException
     * @param request 当前请求（用于日志记录 URI）
     * @return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权")
     */
    @ExceptionHandler(AccessDeniedException.class)
    public AjaxResult handleAccessDeniedException(AccessDeniedException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',权限校验失败'{}'", requestURI, e.getMessage());
        return AjaxResult.error(HttpStatus.FORBIDDEN, "没有权限，请联系管理员授权");
    }

    /**
     * 处理请求方法不支持 —— 返回异常消息。
     *
     * <p>触发场景：如对仅支持 POST 的接口发送 GET 请求。</p>
     *
     * @param e HttpRequestMethodNotSupportedException
     * @param request 当前请求
     * @return AjaxResult.error(e.getMessage())
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public AjaxResult handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e,
            HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',不支持'{}'请求", requestURI, e.getMethod());
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 处理业务层主动抛出的 ServiceException —— 返回自定义错误码和消息。
     *
     * <p>ServiceException 是整个项目最常用的异常基类，
     * 业务层遇到预期之外但可接受的情况（如用户名已存在、验证码错误等）时主动抛出。
     * 若 ServiceException 携带了自定义 code（非默认 500），则使用该 code。</p>
     *
     * @param e ServiceException
     * @param request 当前请求
     * @return AjaxResult（带 code 和 message）
     */
    @ExceptionHandler(ServiceException.class)
    public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
    {
        log.error(e.getMessage(), e);
        Integer code = e.getCode();
        return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
    }

    /**
     * 处理请求路径中缺少必需的路径变量。
     *
     * <p>触发场景：如 @GetMapping("/user/{id}") 但实际请求路径是 /user 或 /user/。</p>
     *
     * @param e MissingPathVariableException
     * @param request 当前请求
     * @return AjaxResult.error("请求路径中缺少必需的路径变量[xxx]")
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public AjaxResult handleMissingPathVariableException(MissingPathVariableException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求路径中缺少必需的路径变量'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求路径中缺少必需的路径变量[%s]", e.getVariableName()));
    }

    /**
     * 处理请求参数类型不匹配 —— 带 XSS 转义防护。
     *
     * <p>触发场景：如期望 Long 类型的 id 参数实际传入了字符串。
     * 由于错误消息中可能包含用户输入的原始值，
     * 必须先通过 {@link EscapeUtil#clean} 转义，防止反射型 XSS。</p>
     *
     * @param e MethodArgumentTypeMismatchException
     * @param request 当前请求
     * @return AjaxResult.error(格式化的类型不匹配提示)
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public AjaxResult handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        String value = Convert.toStr(e.getValue());
        if (StringUtils.isNotEmpty(value))
        {
            value = EscapeUtil.clean(value);
        }
        log.error("请求参数类型不匹配'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(String.format("请求参数类型不匹配，参数[%s]要求类型为：'%s'，但输入值为：'%s'", e.getName(), e.getRequiredType().getName(), value));
    }

    /**
     * 兜底处理 RuntimeException —— 捕获所有非 Spring 自带、非业务层的运行时异常。
     *
     * <p>典型场景：空指针、数组越界、类转换异常等。
     * 这种异常通常意味着代码 Bug，需要重点排查。</p>
     *
     * @param e RuntimeException
     * @param request 当前请求
     * @return AjaxResult.error(e.getMessage())
     */
    @ExceptionHandler(RuntimeException.class)
    public AjaxResult handleRuntimeException(RuntimeException e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生未知异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 最终兜底 —— 捕获所有未被前面 handler 匹配的 Exception。
     *
     * <p>包括 Spring 内置的 HttpMessageNotReadableException、HttpMediaTypeNotSupportedException 等。</p>
     *
     * @param e Exception
     * @param request 当前请求
     * @return AjaxResult.error(e.getMessage())
     */
    @ExceptionHandler(Exception.class)
    public AjaxResult handleException(Exception e, HttpServletRequest request)
    {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return AjaxResult.error(e.getMessage());
    }

    /**
     * 处理 Spring 表单绑定校验异常（@Valid / @Validated 绑定表单 Bean 时触发）。
     *
     * <p>取 {@link BindException#getAllErrors} 中第一个校验错误的 defaultMessage 作为返回消息。</p>
     *
     * @param e BindException
     * @return AjaxResult.error(第一个校验错误消息)
     */
    @ExceptionHandler(BindException.class)
    public AjaxResult handleBindException(BindException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getAllErrors().get(0).getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 处理 Spring @RequestBody 校验异常（@Valid / @Validated 加在 @RequestBody 参数上时触发）。
     *
     * <p>与 {@link #handleBindException} 类似，但触发时机不同：
     * BindException 对应表单提交，MethodArgumentNotValidException 对应 JSON Body。</p>
     *
     * @param e MethodArgumentNotValidException
     * @return AjaxResult.error(第一个字段校验错误消息)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleMethodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        log.error(e.getMessage(), e);
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return AjaxResult.error(message);
    }

    /**
     * 处理演示模式异常。
     *
     * <p>演示模式下禁止修改操作，由业务层主动抛出此异常。</p>
     *
     * @param e DemoModeException
     * @return AjaxResult.error("演示模式，不允许操作")
     */
    @ExceptionHandler(DemoModeException.class)
    public AjaxResult handleDemoModeException(DemoModeException e)
    {
        return AjaxResult.error("演示模式，不允许操作");
    }
}
