package com.ruoyi.framework.config;

import java.util.Random;
import com.google.code.kaptcha.text.impl.DefaultTextCreator;

/**
 * 数学验证码文本生成器 —— 自定义 Kaptcha 文本生成策略
 *
 * <p>设计意图：继承 Kaptcha 的 {@link DefaultTextCreator}，
 * 将普通随机字符替换为数学运算表达式（如 "3*4=?@12"）。
 * 用于 {@link CaptchaConfig#getKaptchaBeanMath} Bean，提供比普通字符验证码更高的人机识别难度。</p>
 *
 * <p>生成规则：</p>
 * <ul>
 *   <li>随机生成两个 0~9 的操作数 x、y</li>
 *   <li>随机选择运算符（0=乘法 *，1=除法 / 或加法 +，2=减法 -）</li>
 *   <li>除法特殊处理：确保被除数能被除数整除（避免小数），否则降级为加法</li>
 *   <li>减法特殊处理：确保结果 ≥ 0（如 x≥y），否则交换操作数顺序</li>
 *   <li>输出格式：{@code 表达式=@结果}（如 {@code 3*4=?@12}），前端展示 "3*4=?"，服务端校验 "@" 后的数值</li>
 * </ul>
 *
 * @author ruoyi
 * @see CaptchaConfig#getKaptchaBeanMath 使用本类作为文本生成器
 */
public class KaptchaTextCreator extends DefaultTextCreator
{
    /** 数字字符数组 0~10（含 10，便于展示） */
    private static final String[] CNUMBERS = "0,1,2,3,4,5,6,7,8,9,10".split(",");

    /**
     * 生成一个数学运算验证码表达式。
     *
     * <p>内部实现：随机数 + if-else 分支选择运算符，
     * 最终输出形如 "x op y=?@result" 的字符串，
     * Kaptcha 图片渲染时会展示 "x op y=?" 部分，
     * 而 result 部分会被存入 Session / Redis 用于后续校验。</p>
     *
     * @return 数学表达式字符串（如 "3*4=?@12"）
     */
    @Override
    public String getText()
    {
        Integer result = 0;
        Random random = new Random();
        int x = random.nextInt(10);
        int y = random.nextInt(10);
        StringBuilder suChinese = new StringBuilder();
        int randomoperands = random.nextInt(3);
        if (randomoperands == 0)
        {
            result = x * y;
            suChinese.append(CNUMBERS[x]);
            suChinese.append("*");
            suChinese.append(CNUMBERS[y]);
        }
        else if (randomoperands == 1)
        {
            if ((x != 0) && y % x == 0)
            {
                result = y / x;
                suChinese.append(CNUMBERS[y]);
                suChinese.append("/");
                suChinese.append(CNUMBERS[x]);
            }
            else
            {
                result = x + y;
                suChinese.append(CNUMBERS[x]);
                suChinese.append("+");
                suChinese.append(CNUMBERS[y]);
            }
        }
        else
        {
            if (x >= y)
            {
                result = x - y;
                suChinese.append(CNUMBERS[x]);
                suChinese.append("-");
                suChinese.append(CNUMBERS[y]);
            }
            else
            {
                result = y - x;
                suChinese.append(CNUMBERS[y]);
                suChinese.append("-");
                suChinese.append(CNUMBERS[x]);
            }
        }
        suChinese.append("=?@" + result);
        return suChinese.toString();
    }
}
