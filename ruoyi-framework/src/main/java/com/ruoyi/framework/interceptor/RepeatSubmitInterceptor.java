package com.ruoyi.framework.interceptor;

import java.lang.reflect.Method;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.annotation.RepeatSubmit;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.utils.ServletUtils;

/**
 * 防重复提交拦截器（抽象基类）
 *
 * <p>设计意图：基于 Spring MVC {@link HandlerInterceptor} 接口，
 * 提供防重复提交的统一框架。子类只需实现 {@link #isRepeatSubmit} 方法，
 * 定义具体的防重提交判断策略。</p>
 *
 * <p>拦截流程：</p>
 * <ol>
 *   <li>在 {@link #preHandle} 中检查目标 Handler 是否为 {@link HandlerMethod}（即 Controller 方法调用）</li>
 *   <li>从方法上查找 {@link RepeatSubmit} 注解 —— 未标注则直接放行</li>
 *   <li>若标注了注解，调用子类实现的 {@link #isRepeatSubmit} 进行判断</li>
 *   <li>若判断为重复提交 → 直接在 Response 中写入 AjaxResult.error(注解指定的 message) 并返回 false，
 *       请求不再进入 Controller</li>
 * </ol>
 *
 * <p>子类实现：</p>
 * <ul>
 *   <li>{@link com.ruoyi.framework.interceptor.impl.SameUrlDataInterceptor} —— 基于 URL + Body 参数比对的防重提交实现</li>
 * </ul>
 *
 * @author ruoyi
 * @see RepeatSubmit 防重提交注解（标注在 Controller 方法上）
 * @see com.ruoyi.framework.interceptor.impl.SameUrlDataInterceptor 具体实现类
 * @see com.ruoyi.framework.config.ResourcesConfig#addInterceptors 拦截器注册入口
 */
@Component
public abstract class RepeatSubmitInterceptor implements HandlerInterceptor
{
    /**
     * Spring MVC 前置拦截器 —— 防重提交检测入口。
     *
     * <p>执行逻辑：只对 Controller 方法请求（HandlerMethod）生效，
     * 其他类型请求（如静态资源、404 等）直接放行。</p>
     *
     * @param request 当前 HTTP 请求
     * @param response 当前 HTTP 响应
     * @param handler Spring MVC 的处理器对象
     * @return true 表示放行；false 表示已拦截并返回防重提交错误响应
     * @throws Exception 子类实现中可能抛出的异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception
    {
        if (handler instanceof HandlerMethod)
        {
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod();
            RepeatSubmit annotation = method.getAnnotation(RepeatSubmit.class);
            if (annotation != null)
            {
                if (this.isRepeatSubmit(request, annotation))
                {
                    AjaxResult ajaxResult = AjaxResult.error(annotation.message());
                    ServletUtils.renderString(response, JSON.toJSONString(ajaxResult));
                    return false;
                }
            }
            return true;
        }
        else
        {
            return true;
        }
    }

    /**
     * 判断当前请求是否属于重复提交 —— 由子类实现具体策略。
     *
     * <p>常见策略示例：</p>
     * <ul>
     *   <li>基于 URL + Body 参数哈希 + 时间窗口 —— 相同参数在短时间内（如 10 秒）重复请求</li>
     *   <li>基于 Token（前端提交唯一 Token，服务端校验后立即失效）</li>
     * </ul>
     *
     * @param request 当前 HTTP 请求（注意：POST Body 流只能读一次，需由 Filter 包装后才可重复读）
     * @param annotation 方法上标注的 {@link RepeatSubmit} 注解（可获取 interval / message 等配置）
     * @return true 表示是重复提交；false 表示不是
     * @throws Exception 实现中可能需要读取请求体等操作
     */
    public abstract boolean isRepeatSubmit(HttpServletRequest request, RepeatSubmit annotation);
}
