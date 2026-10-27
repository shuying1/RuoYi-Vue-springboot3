package com.ruoyi.framework.aspectj;

import java.util.Collection;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NamedThreadLocal;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.enums.BusinessStatus;
import com.ruoyi.common.enums.HttpMethod;
import com.ruoyi.common.filter.PropertyPreExcludeFilter;
import com.ruoyi.common.utils.ExceptionUtil;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.domain.SysOperLog;

/**
 * 操作日志记录切面
 *
 * <p>设计意图：通过 AOP 拦截所有标注了 {@link Log} 注解的 Controller 层方法，
 * 自动采集操作人、请求参数、返回结果、异常信息、耗时等元数据，
 * 构造 {@link SysOperLog} 对象并通过 {@link AsyncManager} 异步写入数据库。
 * 实现业务代码与日志记录的解耦，避免同步日志 I/O 拖慢接口响应。</p>
 *
 * <p>切面执行流程：</p>
 * <ol>
 *   <li>{@code @Before} {@link #doBefore} — 记录方法开始执行时间戳到 ThreadLocal，用于后续计算耗时</li>
 *   <li>{@code @AfterReturning} {@link #doAfterReturning} — 方法正常返回，status 置为 SUCCESS</li>
 *   <li>{@code @AfterThrowing} {@link #doAfterThrowing} — 方法抛出异常，status 置为 FAIL 并记录 errorMsg</li>
 *   <li>统一进入 {@link #handleLog}，组装 SysOperLog 并提交异步任务</li>
 *   <li>finally 中清理 ThreadLocal，防止内存泄漏</li>
 * </ol>
 *
 * <p>参数采集策略：</p>
 * <ul>
 *   <li>GET 请求 — 从 {@link jakarta.servlet.http.HttpServletRequest} 的 ParameterMap 读取</li>
 *   <li>POST/PUT/DELETE — 从 joinPoint.getArgs() 中序列化（自动排除文件上传、HttpServletRequest 等非业务参数）</li>
 *   <li>敏感字段（password 等）通过 {@link PropertyPreExcludeFilter} 自动脱敏</li>
 * </ul>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link AsyncManager} / {@link AsyncFactory} — 日志异步写入工厂</li>
 *   <li>{@link PropertyPreExcludeFilter} — fastjson2 的属性排除过滤器</li>
 * </ul>
 *
 * @author ruoyi
 * @see Log 操作日志注解
 * @see AsyncManager 异步任务管理器
 * @see AsyncFactory#recordOper(SysOperLog) 创建日志记录异步任务
 */
@Aspect
@Component
public class LogAspect
{
    /** 本地异常日志记录器，用于 handleLog 内部捕获到的非预期异常 */
    private static final Logger log = LoggerFactory.getLogger(LogAspect.class);

    /**
     * 需要在日志参数序列化时排除的敏感属性字段，
     * 防止密码等敏感信息被记录到操作日志中。
     */
    public static final String[] EXCLUDE_PROPERTIES = { "password", "oldPassword", "newPassword", "confirmPassword" };

    /**
     * 线程级变量，用于在 @Before 和 @AfterReturning/@AfterThrowing 之间
     * 传递方法开始时间戳，用于计算接口耗时（毫秒）。
     * 使用 {@link NamedThreadLocal} 便于线程调试时的名称展示。
     */
    private static final ThreadLocal<Long> TIME_THREADLOCAL = new NamedThreadLocal<Long>("Cost Time");

    /**
     * 请求参数 / 返回结果序列化的最大长度（字符数），
     * 防止过长的 JSON 报文撑爆数据库字段或日志表。
     */
    private static final int PARAM_MAX_LENGTH = 2000;

    /**
     * 处理请求前执行：记录方法开始时间戳到 ThreadLocal。
     *
     * @param joinPoint AOP 连接点
     * @param controllerLog 目标方法上标注的 {@link Log} 注解实例
     */
    @Before(value = "@annotation(controllerLog)")
    public void doBefore(JoinPoint joinPoint, Log controllerLog)
    {
        TIME_THREADLOCAL.set(System.currentTimeMillis());
    }

    /**
     * 处理完请求后执行（正常返回）。
     *
     * <p>将方法返回值 {@code jsonResult} 传给 {@link #handleLog}，
     * 由其决定是否需要保存到 operLog.jsonResult 字段。</p>
     *
     * @param joinPoint AOP 连接点
     * @param controllerLog 目标方法上的 {@link Log} 注解
     * @param jsonResult 目标方法的返回值，可能为 null
     */
    @AfterReturning(pointcut = "@annotation(controllerLog)", returning = "jsonResult")
    public void doAfterReturning(JoinPoint joinPoint, Log controllerLog, Object jsonResult)
    {
        handleLog(joinPoint, controllerLog, null, jsonResult);
    }

    /**
     * 拦截异常操作：目标方法抛出异常后执行。
     *
     * <p>异常 {@code e} 会被传入 {@link #handleLog}，
     * 用于将 status 置为 FAIL 并记录 errorMsg。</p>
     *
     * @param joinPoint AOP 连接点
     * @param controllerLog 目标方法上的 {@link Log} 注解
     * @param e 目标方法抛出的异常实例
     */
    @AfterThrowing(value = "@annotation(controllerLog)", throwing = "e")
    public void doAfterThrowing(JoinPoint joinPoint, Log controllerLog, Exception e)
    {
        handleLog(joinPoint, controllerLog, e, null);
    }

    /**
     * 操作日志核心处理逻辑 —— 组装 SysOperLog 并异步保存。
     *
     * <p>主要步骤：</p>
     * <ol>
     *   <li>从 {@link SecurityUtils} 获取当前登录用户，写入 operName / deptName</li>
     *   <li>从 {@link IpUtils} / {@link ServletUtils} 获取客户端 IP 和请求 URI</li>
     *   <li>若存在异常，status 置为 FAIL 并截断保存异常消息</li>
     *   <li>拼接 className.methodName 作为操作方法字段</li>
     *   <li>调用 {@link #getControllerMethodDescription} 填充注解元数据、参数和返回值</li>
     *   <li>读取 {@link #TIME_THREADLOCAL} 计算耗时</li>
     *   <li>通过 {@link AsyncManager#execute} 异步落库，避免阻塞接口响应</li>
     *   <li>finally 中清理 TIME_THREADLOCAL，防止线程池复用时的内存泄漏</li>
     * </ol>
     *
     * @param joinPoint AOP 连接点
     * @param controllerLog Log 注解实例
     * @param e 异常（正常返回时为 null）
     * @param jsonResult 返回值（抛出异常时为 null）
     */
    protected void handleLog(final JoinPoint joinPoint, Log controllerLog, final Exception e, Object jsonResult)
    {
        try
        {
            // 获取当前的用户
            LoginUser loginUser = SecurityUtils.getLoginUser();

            // *========数据库日志=========*//
            SysOperLog operLog = new SysOperLog();
            // 默认状态为成功，出现异常时在后面改为失败
            operLog.setStatus(BusinessStatus.SUCCESS.ordinal());
            // 请求的地址
            String ip = IpUtils.getIpAddr();
            operLog.setOperIp(ip);
            operLog.setOperUrl(StringUtils.substring(ServletUtils.getRequest().getRequestURI(), 0, 255));
            if (loginUser != null)
            {
                operLog.setOperName(loginUser.getUsername());
                SysUser currentUser = loginUser.getUser();
                if (StringUtils.isNotNull(currentUser) && StringUtils.isNotNull(currentUser.getDept()))
                {
                    operLog.setDeptName(currentUser.getDept().getDeptName());
                }
            }

            if (e != null)
            {
                // 发生异常，标记失败状态并记录异常消息（截断到 2000 字符）
                operLog.setStatus(BusinessStatus.FAIL.ordinal());
                operLog.setErrorMsg(StringUtils.substring(Convert.toStr(e.getMessage(), ExceptionUtil.getExceptionMessage(e)), 0, 2000));
            }
            // 设置方法名称（全限定类名.方法名()）
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = joinPoint.getSignature().getName();
            operLog.setMethod(className + "." + methodName + "()");
            // 设置请求方式（GET/POST/PUT/DELETE 等）
            operLog.setRequestMethod(ServletUtils.getRequest().getMethod());
            // 处理设置注解上的参数
            getControllerMethodDescription(joinPoint, controllerLog, operLog, jsonResult);
            // 设置消耗时间（当前时间 - 进入切面时记录的时间戳）
            operLog.setCostTime(System.currentTimeMillis() - TIME_THREADLOCAL.get());
            // 保存数据库（异步执行，不阻塞当前请求线程）
            AsyncManager.me().execute(AsyncFactory.recordOper(operLog));
        }
        catch (Exception exp)
        {
            // 记录本地异常日志
            log.error("异常信息:{}", exp.getMessage());
            exp.printStackTrace();
        }
        finally
        {
            // 必须清理 ThreadLocal，防止在线程池复用时脏数据影响后续请求
            TIME_THREADLOCAL.remove();
        }
    }

    /**
     * 获取 {@link Log} 注解中对方法的描述信息，填充到 SysOperLog 中。
     *
     * <p>主要填充：业务类型、标题、操作人类型；
     * 根据注解配置决定是否保存 request 参数和 response 结果。</p>
     *
     * @param joinPoint AOP 连接点
     * @param log Log 注解实例
     * @param operLog 待填充的操作日志实体
     * @param jsonResult 目标方法返回值（可能为 null）
     * @throws Exception 参数序列化过程中可能抛出的异常
     */
    public void getControllerMethodDescription(JoinPoint joinPoint, Log log, SysOperLog operLog, Object jsonResult) throws Exception
    {
        // 设置 action 动作（业务类型枚举的 ordinal 值）
        operLog.setBusinessType(log.businessType().ordinal());
        // 设置标题（注解 title 属性）
        operLog.setTitle(log.title());
        // 设置操作人类别（0=其它,1=后台用户,2=手机端用户）
        operLog.setOperatorType(log.operatorType().ordinal());
        // 是否需要保存 request 参数和值
        if (log.isSaveRequestData())
        {
            // 获取参数的信息，传入到数据库中。
            setRequestValue(joinPoint, operLog, log.excludeParamNames());
        }
        // 是否需要保存 response 参数和值
        if (log.isSaveResponseData() && StringUtils.isNotNull(jsonResult))
        {
            operLog.setJsonResult(StringUtils.substring(JSON.toJSONString(jsonResult), 0, 2000));
        }
    }

    /**
     * 获取请求的参数，放到 operLog.operParam 中。
     *
     * <p>采集策略：</p>
     * <ul>
     *   <li>当 ServletUtils.getParamMap 不为空（如 GET 请求）时，直接序列化 ParameterMap</li>
     *   <li>否则对 POST/PUT/DELETE 请求，通过 {@link #argsArrayToString} 序列化方法入参</li>
     * </ul>
     *
     * @param joinPoint AOP 连接点（用于从 args 中提取 POST body 参数）
     * @param operLog 操作日志实体（会被填充 operParam）
     * @param excludeParamNames 注解中额外指定的需要排除的参数名
     * @throws Exception 参数序列化过程中可能抛出的异常
     */
    private void setRequestValue(JoinPoint joinPoint, SysOperLog operLog, String[] excludeParamNames) throws Exception
    {
        String requestMethod = operLog.getRequestMethod();
        Map<?, ?> paramsMap = ServletUtils.getParamMap(ServletUtils.getRequest());
        if (StringUtils.isEmpty(paramsMap) && StringUtils.equalsAny(requestMethod, HttpMethod.PUT.name(), HttpMethod.POST.name(), HttpMethod.DELETE.name()))
        {
            // POST/PUT/DELETE 请求：从 joinPoint.getArgs() 中序列化参数对象
            String params = argsArrayToString(joinPoint.getArgs(), excludeParamNames);
            operLog.setOperParam(params);
        }
        else
        {
            // GET 请求或有 ParameterMap 的情况：序列化请求参数 Map
            operLog.setOperParam(StringUtils.substring(JSON.toJSONString(paramsMap, excludePropertyPreFilter(excludeParamNames)), 0, PARAM_MAX_LENGTH));
        }
    }

    /**
     * 将方法入参数组拼接为 JSON 字符串，便于保存到操作日志中。
     *
     * <p>跳过非业务参数（文件上传、HttpServletRequest 等），
     * 并通过 {@link PropertyPreExcludeFilter} 排除敏感字段。</p>
     *
     * @param paramsArray 方法入参数组
     * @param excludeParamNames 注解中声明的要额外排除的参数名
     * @return 拼接后的参数字符串，长度上限 {@link #PARAM_MAX_LENGTH}
     */
    private String argsArrayToString(Object[] paramsArray, String[] excludeParamNames)
    {
        StringBuilder params = new StringBuilder();
        if (paramsArray != null && paramsArray.length > 0)
        {
            for (Object o : paramsArray)
            {
                if (StringUtils.isNotNull(o) && !isFilterObject(o))
                {
                    try
                    {
                        String jsonObj = JSON.toJSONString(o, excludePropertyPreFilter(excludeParamNames));
                        params.append(jsonObj).append(" ");
                        // 提前截断：避免无效拼接过长的 JSON 字符串
                        if (params.length() >= PARAM_MAX_LENGTH)
                        {
                            return StringUtils.substring(params.toString(), 0, PARAM_MAX_LENGTH);
                        }
                    }
                    catch (Exception e)
                    {
                        // 单个参数序列化失败不影响其他参数，仅记录日志
                        log.error("请求参数拼装异常 msg:{}, 参数:{}", e.getMessage(), paramsArray, e);
                    }
                }
            }
        }
        return params.toString();
    }

    /**
     * 构建敏感属性排除过滤器。
     *
     * <p>将固定敏感字段（password 等）与注解指定的额外排除字段合并，
     * 用于 fastjson2 序列化时自动过滤。</p>
     *
     * @param excludeParamNames Log 注解中额外声明的要排除的字段名
     * @return fastjson2 的属性预排除过滤器实例
     */
    public PropertyPreExcludeFilter excludePropertyPreFilter(String[] excludeParamNames)
    {
        return new PropertyPreExcludeFilter().addExcludes(ArrayUtils.addAll(EXCLUDE_PROPERTIES, excludeParamNames));
    }

    /**
     * 判断参数对象是否需要在日志序列化时被过滤掉。
     *
     * <p>需要过滤的类型：</p>
     * <ul>
     *   <li>{@link MultipartFile} 及其数组（文件上传参数）</li>
     *   <li>{@link HttpServletRequest} / {@link HttpServletResponse}（Servlet 容器对象）</li>
     *   <li>{@link BindingResult}（Spring 校验结果）</li>
     *   <li>包含以上对象的 Collection / Map</li>
     * </ul>
     *
     * @param o 待判断的对象
     * @return true 表示需要过滤（不应序列化到日志中）
     */
    @SuppressWarnings("rawtypes")
    public boolean isFilterObject(final Object o)
    {
        Class<?> clazz = o.getClass();
        if (clazz.isArray())
        {
            return clazz.getComponentType().isAssignableFrom(MultipartFile.class);
        }
        else if (Collection.class.isAssignableFrom(clazz))
        {
            Collection collection = (Collection) o;
            for (Object value : collection)
            {
                return value instanceof MultipartFile;
            }
        }
        else if (Map.class.isAssignableFrom(clazz))
        {
            Map map = (Map) o;
            for (Object value : map.entrySet())
            {
                Map.Entry entry = (Map.Entry) value;
                return entry.getValue() instanceof MultipartFile;
            }
        }
        return o instanceof MultipartFile || o instanceof HttpServletRequest || o instanceof HttpServletResponse
                || o instanceof BindingResult;
    }
}
