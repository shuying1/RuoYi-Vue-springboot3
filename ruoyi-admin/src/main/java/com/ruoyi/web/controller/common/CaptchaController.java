package com.ruoyi.web.controller.common;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.Resource;
import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FastByteArrayOutputStream;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.google.code.kaptcha.Producer;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.sign.Base64;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 验证码接口控制器
 * 
 * <p>对外提供登录与注册阶段所需的图形验证码。验证码图片由 Kaptcha 库生成，
 * 支持两种类型：普通字符验证码（{@code char}）与数学运算验证码（{@code math}），
 * 具体类型由 {@link RuoYiConfig#getCaptchaType()} 配置决定。</p>
 * 
 * <p>生成的验证码答案以 {@link Constants#CAPTCHA_EXPIRATION} 分钟为 TTL 写入 Redis，
 * key 前缀为 {@link CacheConstants#CAPTCHA_CODE_KEY}，并随响应一并返回 uuid 供后续登录校验使用。</p>
 * 
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET /captchaImage} — 获取验证码图片（Base64 编码）和唯一标识符 uuid</li>
 * </ul>
 * 
 * @author ruoyi
 */
@RestController
public class CaptchaController
{
    /** 普通字符验证码生成器 Bean，名称由配置类中 {@code captchaProducer} 注册 */
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    /** 数学运算验证码生成器 Bean，名称由配置类中 {@code captchaProducerMath} 注册 */
    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    /** Redis 缓存客户端，用于存储验证码答案 */
    @Autowired
    private RedisCache redisCache;
    
    /** 系统参数配置服务，用于读取 {@code sys.account.captchaEnabled} 是否开启验证码 */
    @Autowired
    private ISysConfigService configService;

    /**
     * 获取验证码图片
     * 
     * <p>GET /captchaImage</p>
     * 
     * <p>响应 {@link AjaxResult#success()}：</p>
     * <pre>
     * {
     *   "captchaEnabled": true,        // 当前是否启用验证码（由系统参数配置）
     *   "uuid": "a1b2c3d4...",         // 本次验证码唯一标识，登录时需携带
     *   "img": "data:image/jpg;base64," // 验证码图片 Base64 编码字符串
     * }
     * </pre>
     * 
     * @param response HTTP 响应对象，用于写入图片流（实际实现中未直接使用，图片走 JSON 返回）
     * @return AjaxResult 包含验证码开关、uuid、Base64 图片；若禁用验证码则只返回 captchaEnabled
     * @throws IOException 图片转换过程中的 IO 异常
     */
    @GetMapping("/captchaImage")
    public AjaxResult getCode(HttpServletResponse response) throws IOException
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            return ajax;
        }

        // 保存验证码信息
        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;

        String capStr = null, code = null;
        BufferedImage image = null;

        // 生成验证码
        String captchaType = RuoYiConfig.getCaptchaType();
        if ("math".equals(captchaType))
        {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        }
        else if ("char".equals(captchaType))
        {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisCache.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        // 转换流信息写出
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        try
        {
            ImageIO.write(image, "jpg", os);
        }
        catch (IOException e)
        {
            return AjaxResult.error(e.getMessage());
        }

        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }
}
