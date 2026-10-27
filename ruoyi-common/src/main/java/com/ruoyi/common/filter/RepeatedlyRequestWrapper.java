package com.ruoyi.common.filter;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import com.ruoyi.common.utils.http.HttpHelper;
import com.ruoyi.common.constant.Constants;

/**
 * 可重复读取请求体的 HttpServletRequest 包装器
 *
 * <p>继承 {@link HttpServletRequestWrapper}，在构造函数中一次性将请求体全部读取到
 * 内部 byte[] body 中，之后每次调用 getInputStream()/getReader() 都从同一个 body 字节数组
 * 创建新的 ByteArrayInputStream 返回，从而实现 Body 的重复读取。
 *
 * <p>构造时同时设置 request 和 response 的 UTF-8 字符集，避免中文乱码。
 * 被 {@link RepeatableFilter} 使用。
 *
 * <p>技术栈：Jakarta Servlet
 *
 * @author ruoyi
 */
public class RepeatedlyRequestWrapper extends HttpServletRequestWrapper
{
    private final byte[] body;

    public RepeatedlyRequestWrapper(HttpServletRequest request, ServletResponse response) throws IOException
    {
        super(request);
        request.setCharacterEncoding(Constants.UTF8);
        response.setCharacterEncoding(Constants.UTF8);

        body = HttpHelper.getBodyString(request).getBytes(Constants.UTF8);
    }

    @Override
    public BufferedReader getReader() throws IOException
    {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }

    @Override
    public ServletInputStream getInputStream() throws IOException
    {
        final ByteArrayInputStream bais = new ByteArrayInputStream(body);
        return new ServletInputStream()
        {
            @Override
            public int read() throws IOException
            {
                return bais.read();
            }

            @Override
            public int available() throws IOException
            {
                return body.length;
            }

            @Override
            public boolean isFinished()
            {
                return false;
            }

            @Override
            public boolean isReady()
            {
                return false;
            }

            @Override
            public void setReadListener(ReadListener readListener)
            {

            }
        };
    }
}
