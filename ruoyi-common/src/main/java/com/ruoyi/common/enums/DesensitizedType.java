package com.ruoyi.common.enums;

import java.util.function.Function;
import com.ruoyi.common.utils.DesensitizedUtil;

/**
 * 数据脱敏类型枚举
 * 
 * 配合 {@link com.ruoyi.common.annotation.Sensitive} 注解使用，
 * 每种脱敏类型封装一个 {@link Function} 函数式接口，定义具体的脱敏规则。
 *
 * 支持的脱敏场景：姓名、密码、身份证号、手机号、邮箱、银行卡号、车牌号。
 * 
 * @author ruoyi
 */
public enum DesensitizedType
{
    /**
     * 姓名脱敏：保留第1位，第2位起用*号替换。如"张三"→"张*"
     */
    USERNAME(s -> s.replaceAll("(\\S)\\S(\\S*)", "$1*$2")),

    /**
     * 密码脱敏：全部字符用*代替。如"123456"→"******"
     */
    PASSWORD(DesensitizedUtil::password),

    /**
     * 身份证号脱敏：中间10位用*替换，保留前4后4位。如"110101199001011234"→"1101** **** ****1234"
     */
    ID_CARD(s -> s.replaceAll("(\\d{4})\\d{10}(\\d{3}[Xx]|\\d{4})", "$1** **** ****$2")),

    /**
     * 手机号脱敏：中间4位用*替换。如"13812345678"→"138****5678"
     */
    PHONE(s -> s.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2")),

    /**
     * 邮箱脱敏：仅显示第一个字符和@后面的部分。如"test@example.com"→"t****@example.com"
     */
    EMAIL(s -> s.replaceAll("(^.)[^@]*(@.*$)", "$1****$2")),

    /**
     * 银行卡号脱敏：保留最后4位，其他用*替换。如"6222021234567890123"→"**** **** **** **** 0123"
     */
    BANK_CARD(s -> s.replaceAll("\\d{15}(\\d{3})", "**** **** **** **** $1")),

    /**
     * 车牌号脱敏：保留前2位和后3位，中间用*替换，兼容普通车牌和新能源车牌
     */
    CAR_LICENSE(DesensitizedUtil::carLicense);

    /**
     * 脱敏处理函数，接收原始字符串返回脱敏后字符串
     */
    private final Function<String, String> desensitizer;

    /**
     * 构造函数
     * 
     * @param desensitizer 脱敏函数式接口
     */
    DesensitizedType(Function<String, String> desensitizer)
    {
        this.desensitizer = desensitizer;
    }

    /**
     * 获取脱敏处理函数
     * 
     * @return 脱敏函数
     */
    public Function<String, String> desensitizer()
    {
        return desensitizer;
    }
}
