package com.ruoyi.common.constant;

/**
 * 用户相关常量信息类
 * 
 * 定义系统中用户、角色、部门、字典、菜单等模块的状态值、类型标识、
 * 前端组件名称、校验常量等系统级业务常量。
 * 
 * @author ruoyi
 */
public class UserConstants
{
    /**
     * 平台内系统用户的唯一标志，用于区分平台管理用户和第三方系统用户
     */
    public static final String SYS_USER = "SYS_USER";

    /**
     * 正常状态值，用户/角色/部门/字典处于启用状态时的值 "0"
     */
    public static final String NORMAL = "0";

    /**
     * 异常或停用状态值 "1"，与 {@link #NORMAL} 对应
     */
    public static final String EXCEPTION = "1";

    /**
     * 用户封禁状态值 "1"，用于sys_user表status字段
     */
    public static final String USER_DISABLE = "1";

    /**
     * 角色正常状态值 "0"，用于sys_role表status字段
     */
    public static final String ROLE_NORMAL = "0";

    /**
     * 角色封禁状态值 "1"，用于sys_role表status字段
     */
    public static final String ROLE_DISABLE = "1";

    /**
     * 部门正常状态值 "0"，用于sys_dept表status字段
     */
    public static final String DEPT_NORMAL = "0";

    /**
     * 部门停用状态值 "1"，用于sys_dept表status字段
     */
    public static final String DEPT_DISABLE = "1";

    /**
     * 字典正常状态值 "0"，用于sys_dict_type和sys_dict_data表status字段
     */
    public static final String DICT_NORMAL = "0";

    /**
     * 是否为系统默认值（是）"Y"，系统默认数据不允许删除
     */
    public static final String YES = "Y";

    /**
     * 菜单为外链类型标识（是）"0"，菜单地址为外部链接
     */
    public static final String YES_FRAME = "0";

    /**
     * 菜单非外链类型标识（否）"1"，菜单为内部路由
     */
    public static final String NO_FRAME = "1";

    /**
     * 菜单类型：目录（M），用于菜单树形结构中的分组节点
     */
    public static final String TYPE_DIR = "M";

    /**
     * 菜单类型：菜单（C），对应前端的实际页面路由
     */
    public static final String TYPE_MENU = "C";

    /**
     * 菜单类型：按钮（F），对应页面上的操作权限按钮
     */
    public static final String TYPE_BUTTON = "F";

    /**
     * 前端Layout布局组件标识，用于菜单路由配置中标识主布局组件
     */
    public final static String LAYOUT = "Layout";
    
    /**
     * 前端ParentView父级路由组件标识，用于多级菜单嵌套场景
     */
    public final static String PARENT_VIEW = "ParentView";

    /**
     * 前端InnerLink内嵌链接组件标识，用于在iframe中展示外部链接
     */
    public final static String INNER_LINK = "InnerLink";

    /**
     * 校验唯一性操作的返回标识 true
     */
    public final static boolean UNIQUE = true;

    /**
     * 校验唯一性操作的返回标识 false
     */
    public final static boolean NOT_UNIQUE = false;

    /**
     * 用户名最小长度限制，默认2个字符
     */
    public static final int USERNAME_MIN_LENGTH = 2;

    /**
     * 用户名最大长度限制，默认20个字符
     */
    public static final int USERNAME_MAX_LENGTH = 20;

    /**
     * 密码最小长度限制，默认5个字符
     */
    public static final int PASSWORD_MIN_LENGTH = 5;

    /**
     * 密码最大长度限制，默认20个字符
     */
    public static final int PASSWORD_MAX_LENGTH = 20;
}
