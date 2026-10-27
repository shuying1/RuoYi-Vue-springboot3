package com.ruoyi.common.filter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.html.EscapeUtil;

/**
 * XSS 清理用 HttpServletRequest 包装器
 *
 * <p>继承 {@link HttpServletRequestWrapper}，覆盖 getParameterValues() 和 getInputStream()
 * 两个关键入口，分别对普通表单参数和 JSON Body 中的值调用 {@link com.ruoyi.common.utils.html.EscapeUtil#clean}
 * 实现 HTML 转义（script、iframe、事件属性等），防止反射型 XSS 攻击。
 *
 * <p>getInputStream() 中先将 JSON Body 全量读取到内存，清理后重新包装为 ByteArrayInputStream 返回，
 * 保证后续 Spring MVC 可以正常读取。非 JSON 请求直接透传原始流。
 *
 * <p>技术栈：Jakarta Servlet、Apache Commons IO
 *
 * @author ruoyi
 */
public class XssHttpServletRequestWrapper extends HttpServletRequestWrapper
{
    /**
     * @param request
     */
    public XssHttpServletRequestWrapper(HttpServletRequest request)
    {
        super(request);
    }

    @Override
    public String[] getParameterValues(String name)
    {
        String[] values = super.getParameterValues(name);
        if (values != null)
        {
            int length = values.length;
            String[] escapesValues = new String[length];
            for (int i = 0; i < length; i++)
            {
                // 防xss攻击和过滤前后空格
                escapesValues[i] = EscapeUtil.clean(values[i]).trim();
            }
            return escapesValues;
        }
        return super.getParameterValues(name);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        // 非json类型，直接返回
        if (!isJsonRequest())
        {
            return super.getInputStream();
        }

        // 为空，直接返回
        String json = IOUtils.toString(super.getInputStream(), "utf-8");
        if (StringUtils.isEmpty(json))
        {
            return super.getInputStream();
        }

        // xss过滤
        json = EscapeUtil.clean(json).trim();
        byte[] jsonBytes = json.getBytes("utf-8");
        final ByteArrayInputStream bis = new ByteArrayInputStream(jsonBytes);
        return new ServletInputStream()
        {
            @Override
            public boolean isFinished()
            {
                return true;
            }

            @Override
            public boolean isReady()
            {
                return true;
            }

            @Override
            public int available() throws IOException
            {
                return jsonBytes.length;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {
            }

            @Override
            public int read() throws IOException
            {
                return bis.read();
            }
        };
    }

    /**
     * 是否是Json请求
     * 
     * @param request
     */
    public boolean isJsonRequest()
    {
        String header = super.getHeader(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }
}