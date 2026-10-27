package com.ruoyi.common.constant;

import java.util.Locale;
import io.jsonwebtoken.Claims;

/**
 * 系统通用常量信息
 * 
 * 定义系统全局使用的各种常量，包括字符集、协议标识、登录相关、权限标识、JWT令牌字段、
 * 安全白名单配置、远程方法调用前缀、以及部门数据权限范围等常量值。
 *
 * 该类所有常量均为不可变的public static final字段，可在系统任何位置直接引用。
 * 内部类 {@link Dept} 封装了部门数据权限范围的五个等级常量。
 * 
 * @author ruoyi
 */
public class Constants
{
    /**
     * UTF-8 字符集编码名称，广泛用于字符串编解码、文件读写场景
     */
    public static final String UTF8 = "UTF-8";

    /**
     * GBK 字符集编码名称，用于兼容中文Windows环境下的老式应用
     */
    public static final String GBK = "GBK";

    /**
     * 系统默认语言环境，固定为简体中文 {@link Locale#SIMPLIFIED_CHINESE}
     */
    public static final Locale DEFAULT_LOCALE = Locale.SIMPLIFIED_CHINESE;

    /**
     * URL中www子域名前缀，用于域名处理场景
     */
    public static final String WWW = "www.";

    /**
     * HTTP协议URL前缀
     */
    public static final String HTTP = "http://";

    /**
     * HTTPS协议URL前缀
     */
    public static final String HTTPS = "https://";

    /**
     * 通用操作成功标识值 "0"，用于AjaxResult、Service层返回码等场景
     */
    public static final String SUCCESS = "0";

    /**
     * 通用操作失败标识值 "1"，与 {@link #SUCCESS} 对应
     */
    public static final String FAIL = "1";

    /**
     * 登录操作成功日志标识，用于操作日志记录场景
     */
    public static final String LOGIN_SUCCESS = "Success";

    /**
     * 用户注销操作日志标识
     */
    public static final String LOGOUT = "Logout";

    /**
     * 用户注册操作日志标识
     */
    public static final String REGISTER = "Register";

    /**
     * 登录操作失败日志标识
     */
    public static final String LOGIN_FAIL = "Error";

    /**
     * 通配权限标识，值为 "*:*:*"，表示拥有系统所有权限
     */
    public static final String ALL_PERMISSION = "*:*:*";

    /**
     * 超级管理员角色名称标识，值为 "admin"
     */
    public static final String SUPER_ADMIN = "admin";

    /**
     * 角色权限字符串之间的分隔符（英文逗号）
     */
    public static final String ROLE_DELIMITER = ",";

    /**
     * 权限标识字符串之间的分隔符（英文逗号）
     */
    public static final String PERMISSION_DELIMITER = ",";

    /**
     * 图形验证码有效期，单位为分钟，默认2分钟后过期
     */
    public static final Integer CAPTCHA_EXPIRATION = 2;

    /**
     * 请求头中携带令牌的字段名，值为 "token"
     */
    public static final String TOKEN = "token";

    /**
     * JWT令牌前缀，格式为 "Bearer "，用于Authorization请求头
     */
    public static final String TOKEN_PREFIX = "Bearer ";

    /**
     * 登录用户在Redis中缓存的键前缀，值为 "login_user_key"
     */
    public static final String LOGIN_USER_KEY = "login_user_key";

    /**
     * JWT载荷中用户ID的字段名，值为 "userid"
     */
    public static final String JWT_USERID = "userid";

    /**
     * JWT载荷中用户名的字段名，复用JWT标准主题字段 {@link Claims#SUBJECT}
     */
    public static final String JWT_USERNAME = Claims.SUBJECT;

    /**
     * JWT载荷中用户头像的字段名，值为 "avatar"
     */
    public static final String JWT_AVATAR = "avatar";

    /**
     * JWT载荷中令牌创建时间的字段名，值为 "created"
     */
    public static final String JWT_CREATED = "created";

    /**
     * JWT载荷中用户权限集合的字段名，值为 "authorities"
     */
    public static final String JWT_AUTHORITIES = "authorities";

    /**
     * 本地静态资源访问路径映射前缀，值为 "/profile"
     */
    public static final String RESOURCE_PREFIX = "/profile";

    /**
     * RMI远程方法调用协议前缀，用于反序列化安全白名单
     */
    public static final String LOOKUP_RMI = "rmi:";

    /**
     * LDAP远程方法调用协议前缀，用于反序列化安全白名单
     */
    public static final String LOOKUP_LDAP = "ldap:";

    /**
     * LDAPS安全远程方法调用协议前缀，用于反序列化安全白名单
     */
    public static final String LOOKUP_LDAPS = "ldaps:";

    /**
     * 自动识别JSON对象白名单配置（仅允许解析的包名，范围越小越安全）
     * 当前仅允许 com.ruoyi 包下的类被自动反序列化
     */
    public static final String[] JSON_WHITELIST_STR = { "com.ruoyi" };

    /**
     * 定时任务执行白名单配置（仅允许访问的包名，如其他需要可以自行添加）
     * 当前仅允许 com.ruoyi.quartz.task 包下的类作为定时任务目标
     */
    public static final String[] JOB_WHITELIST_STR = { "com.ruoyi.quartz.task" };

    /**
     * 定时任务违规字符/类名黑名单，包含常见的远程代码执行、反序列化攻击载体
     * 匹配到这些关键字会阻止定时任务执行，防止安全漏洞
     */
    public static final String[] JOB_ERROR_STR = { "java.net.URL", "javax.naming.InitialContext", "org.yaml.snakeyaml",
            "org.springframework", "org.apache", "com.ruoyi.common.utils.file", "com.ruoyi.common.config", "com.ruoyi.generator" };

    /**
     * 部门数据权限范围常量内部类
     * 
     * 定义了数据权限的五个等级，用于SQL动态拼接时根据当前用户角色过滤数据范围。
     * 权限值存储在 sys_role 表的 data_scope 字段中。
     */
    public static class Dept
    {
        /**
         * 全部数据权限（值为"1"）：可查看和操作系统所有数据
         */
        public static final String DATA_SCOPE_ALL = "1";

        /**
         * 自定义数据权限（值为"2"）：根据用户关联的部门ID列表过滤数据
         */
        public static final String DATA_SCOPE_CUSTOM = "2";

        /**
         * 本部门数据权限（值为"3"）：仅可查看和操作本部门的数据
         */
        public static final String DATA_SCOPE_DEPT = "3";

        /**
         * 本部门及以下数据权限（值为"4"）：可查看本部门及所有子部门的数据
         */
        public static final String DATA_SCOPE_DEPT_AND_CHILD = "4";

        /**
         * 仅本人数据权限（值为"5"）：仅可查看和操作自己创建或所属的数据
         */
        public static final String DATA_SCOPE_SELF = "5";
    }
}
