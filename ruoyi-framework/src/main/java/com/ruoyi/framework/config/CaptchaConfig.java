package com.ruoyi.framework.config;

import java.util.Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import static com.google.code.kaptcha.Constants.*;

/**
 * Kaptcha 验证码配置 —— 两种验证码 Bean
 *
 * <p>设计意图：基于 Google Kaptcha 库提供图形验证码生成能力，
 * 注册两个 {@link DefaultKaptcha} Bean：</p>
 * <ul>
 *   <li>{@code captchaProducer} —— 普通随机字符验证码（4 位），
 *       用于常规登录场景</li>
 *   <li>{@code captchaProducerMath} —— 数学运算验证码（如 "3*4=?@12"），
 *       使用自定义的 {@link KaptchaTextCreator} 作为文本生成器，
 *       提供更高的人机识别难度</li>
 * </ul>
 *
 * <p>Kaptcha 配置项说明：</p>
 * <ul>
 *   <li>KAPTCHA_BORDER —— 是否有边框</li>
 *   <li>KAPTCHA_TEXTPRODUCER_FONT_COLOR / SIZE / NAMES —— 字体颜色 / 大小 / 字体族</li>
 *   <li>KAPTCHA_IMAGE_WIDTH / HEIGHT —— 验证码图片尺寸（px）</li>
 *   <li>KAPTCHA_TEXTPRODUCER_CHAR_LENGTH —— 验证码字符长度</li>
 *   <li>KAPTCHA_OBSCURIFICATOR_IMPL —— 图片干扰样式（水纹 / 鱼眼 / 阴影）</li>
 * </ul>
 *
 * @author ruoyi
 * @see KaptchaTextCreator 数学验证码文本生成器
 * @see DefaultKaptcha Kaptcha 默认验证码实现
 */
@Configuration
public class CaptchaConfig
{
    /**
     * 普通字符验证码 Bean。
     *
     * <p>生成 4 位随机字符（默认 Kaptcha 文本生成器），图片尺寸 160×60px，
     * 阴影干扰样式。</p>
     *
     * @return 配置好的 DefaultKaptcha 实例
     */
    @Bean(name = "captchaProducer")
    public DefaultKaptcha getKaptchaBean()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 是否有边框 默认为true 我们可以自己设置yes，no
        properties.setProperty(KAPTCHA_BORDER, "yes");
        // 验证码文本字符颜色 默认为Color.BLACK
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, "black");
        // 验证码图片宽度 默认为200
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, "160");
        // 验证码图片高度 默认为50
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, "60");
        // 验证码文本字符大小 默认为40
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, "38");
        // KAPTCHA_SESSION_KEY
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCode");
        // 验证码文本字符长度 默认为5
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "4");
        // 验证码文本字体样式 默认为new Font("Arial", 1, fontSize), new Font("Courier", 1, fontSize)
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial,Courier");
        // 图片样式 水纹com.google.code.kaptcha.impl.WaterRipple 鱼眼com.google.code.kaptcha.impl.FishEyeGimpy 阴影com.google.code.kaptcha.impl.ShadowGimpy
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.ShadowGimpy");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }

    /**
     * 数学运算验证码 Bean。
     *
     * <p>使用自定义 {@link KaptchaTextCreator} 生成数学表达式（如 "3*4=?@12"），
     * 图片尺寸 160×60px，无边框噪点，阴影干扰。</p>
     *
     * @return 配置好的 DefaultKaptcha 实例（数学验证码版本）
     */
    @Bean(name = "captchaProducerMath")
    public DefaultKaptcha getKaptchaBeanMath()
    {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 是否有边框 默认为true 我们可以自己设置yes，no
        properties.setProperty(KAPTCHA_BORDER, "yes");
        // 边框颜色 默认为Color.BLACK
        properties.setProperty(KAPTCHA_BORDER_COLOR, "105,179,90");
        // 验证码文本字符颜色 默认为Color.BLACK
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_COLOR, "blue");
        // 验证码图片宽度 默认为200
        properties.setProperty(KAPTCHA_IMAGE_WIDTH, "160");
        // 验证码图片高度 默认为50
        properties.setProperty(KAPTCHA_IMAGE_HEIGHT, "60");
        // 验证码文本字符大小 默认为40
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_SIZE, "35");
        // KAPTCHA_SESSION_KEY
        properties.setProperty(KAPTCHA_SESSION_CONFIG_KEY, "kaptchaCodeMath");
        // 验证码文本生成器
        properties.setProperty(KAPTCHA_TEXTPRODUCER_IMPL, "com.ruoyi.framework.config.KaptchaTextCreator");
        // 验证码文本字符间距 默认为2
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_SPACE, "3");
        // 验证码文本字符长度 默认为5
        properties.setProperty(KAPTCHA_TEXTPRODUCER_CHAR_LENGTH, "6");
        // 验证码文本字体样式 默认为new Font("Arial", 1, fontSize), new Font("Courier", 1, fontSize)
        properties.setProperty(KAPTCHA_TEXTPRODUCER_FONT_NAMES, "Arial,Courier");
        // 验证码噪点颜色 默认为Color.BLACK
        properties.setProperty(KAPTCHA_NOISE_COLOR, "white");
        // 干扰实现类
        properties.setProperty(KAPTCHA_NOISE_IMPL, "com.google.code.kaptcha.impl.NoNoise");
        // 图片样式 水纹com.google.code.kaptcha.impl.WaterRipple 鱼眼com.google.code.kaptcha.impl.FishEyeGimpy 阴影com.google.code.kaptcha.impl.ShadowGimpy
        properties.setProperty(KAPTCHA_OBSCURIFICATOR_IMPL, "com.google.code.kaptcha.impl.ShadowGimpy");
        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}
