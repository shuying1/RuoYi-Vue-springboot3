package com.ruoyi.common.utils.http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.ServletRequest;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP 请求辅助工具类
 *
 * <p>封装 HttpURLConnection 的请求体读取、响应体读取、URL 参数拼接等底层操作，
 * 是 {@link HttpUtils} 的底层支撑。
 *
 * <p>核心方法：
 * <ul>
 *   <li>getBodyString：从 HttpServletRequest/HttpURLConnection 中完整读取请求/响应体</li>
 *   <li>encodeUrl：URL 参数 UTF-8 编码拼接</li>
 * </ul>
 *
 * <p>技术栈：JDK HttpURLConnection
 *
 * @author ruoyi
 */
public class HttpHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);

    public static String getBodyString(ServletRequest request)
    {
        StringBuilder sb = new StringBuilder();
        BufferedReader reader = null;
        try (InputStream inputStream = request.getInputStream())
        {
            reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line = "";
            while ((line = reader.readLine()) != null)
            {
                sb.append(line);
            }
        }
        catch (IOException e)
        {
            LOGGER.warn("getBodyString出现问题！");
        }
        finally
        {
            if (reader != null)
            {
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    LOGGER.error(ExceptionUtils.getMessage(e));
                }
            }
        }
        return sb.toString();
    }
}
