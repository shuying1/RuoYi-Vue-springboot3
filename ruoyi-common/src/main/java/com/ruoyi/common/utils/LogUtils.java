package com.ruoyi.common.utils;

/**
 * 操作日志记录工具类
 *
 * <p>配合 @Log 注解使用，在 AOP 切面中根据注解配置组装 {@link com.ruoyi.common.core.domain.OperLog} 对象，
 * 调用异步服务将操作日志持久化到数据库。
 *
 * <p>主要能力：构建登录/登出日志、构建操作日志（含请求参数和响应结果）、设置客户端 IP 和 UA。
 *
 * @author ruoyi
 */
public class LogUtils
{
    public static String getBlock(Object msg)
    {
        if (msg == null)
        {
            msg = "";
        }
        return "[" + msg.toString() + "]";
    }
}
