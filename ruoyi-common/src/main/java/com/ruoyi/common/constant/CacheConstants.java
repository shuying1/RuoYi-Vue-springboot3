package com.ruoyi.common.constant;

/**
 * Redis缓存键名常量类
 * 
 * 定义系统中使用Redis缓存时的各种键前缀，用于区分不同业务场景的缓存数据。
 * 所有键值均采用冒号(:)分隔的分层命名规范，便于Redis Key的批量匹配和管理。
 * 
 * @author ruoyi
 */
public class CacheConstants
{
    /**
     * 登录用户Redis缓存键前缀，完整格式为 "login_tokens:{uuid}"，存储LoginUser对象
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 图形验证码Redis缓存键前缀，完整格式为 "captcha_codes:{uuid}"，存储验证码文本值
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理缓存键前缀，完整格式为 "sys_config:{configKey}"，存储系统配置参数值
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理缓存键前缀，完整格式为 "sys_dict:{dictType}"，存储字典数据列表
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 防重提交Redis缓存键前缀，完整格式为 "repeat_submit:{md5}"，基于请求参数摘要防止重复表单提交
     */
    public static final String REPEAT_SUBMIT_KEY = "repeat_submit:";

    /**
     * 接口限流Redis缓存键前缀，完整格式为 "rate_limit:{key}"，用于RateLimiter注解的限流控制
     */
    public static final String RATE_LIMIT_KEY = "rate_limit:";

    /**
     * 登录账户密码错误次数Redis缓存键前缀，完整格式为 "pwd_err_cnt:{username}"，用于密码重试次数限制
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";
}
