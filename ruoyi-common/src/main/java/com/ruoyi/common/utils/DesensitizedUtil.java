package com.ruoyi.common.utils;

/**
 * 敏感信息脱敏工具类
 *
 * <p>提供密码、车牌号等场景的脱敏处理方法，是 {@link com.ruoyi.common.enums.DesensitizedType}
 * 枚举中 PASSWORD 和 CAR_LICENSE 两个脱敏函数的具体实现。其余脱敏规则（手机号、身份证、邮箱、银行卡号、姓名）
 * 通过正则直接在枚举中定义。
 *
 * <p>车牌号脱敏：对标准格式（如京A12345）保留首位汉字（省简称）和最后3位，其余用 * 替换；
 * 新能源车牌（如京AD12345）同样保留首位和最后3位。
 *
 * @author ruoyi
 */
public class DesensitizedUtil
{
    /**
     * 密码的全部字符都用*代替，比如：******
     *
     * @param password 密码
     * @return 脱敏后的密码
     */
    public static String password(String password)
    {
        if (StringUtils.isBlank(password))
        {
            return StringUtils.EMPTY;
        }
        return StringUtils.repeat('*', password.length());
    }

    /**
     * 车牌中间用*代替，如果是错误的车牌，不处理
     *
     * @param carLicense 完整的车牌号
     * @return 脱敏后的车牌
     */
    public static String carLicense(String carLicense)
    {
        if (StringUtils.isBlank(carLicense))
        {
            return StringUtils.EMPTY;
        }
        // 普通车牌
        if (carLicense.length() == 7)
        {
            carLicense = StringUtils.hide(carLicense, 3, 6);
        }
        else if (carLicense.length() == 8)
        {
            // 新能源车牌
            carLicense = StringUtils.hide(carLicense, 3, 7);
        }
        return carLicense;
    }
}
