package com.ruoyi.common.constant;

/**
 * 定时任务调度通用常量类
 * 
 * 定义Quartz定时任务模块使用的各种常量，包括调度器Map中的属性键名、
 * 调度失败补偿策略（misfire）的四种处理方式，以及任务运行状态枚举。
 * 
 * @author ruoyi
 */
public class ScheduleConstants
{
    /**
     * 定时任务调度时在JobDataMap中存储任务类名的键
     */
    public static final String TASK_CLASS_NAME = "TASK_CLASS_NAME";

    /**
     * 定时任务调度时在JobDataMap中存储任务执行参数的键
     */
    public static final String TASK_PROPERTIES = "TASK_PROPERTIES";

    /**
     * 调度失败默认策略（值"0"）：立即执行一次补偿任务
     */
    public static final String MISFIRE_DEFAULT = "0";

    /**
     * 忽略所有调度失败立即触发执行策略（值"1"）
     */
    public static final String MISFIRE_IGNORE_MISFIRES = "1";

    /**
     * 触发一次执行后按正常调度继续策略（值"2"）
     */
    public static final String MISFIRE_FIRE_AND_PROCEED = "2";

    /**
     * 不触发立即执行，等待下一次正常调度策略（值"3"）
     */
    public static final String MISFIRE_DO_NOTHING = "3";

    /**
     * 定时任务运行状态枚举
     * 
     * 定义定时任务在数据库中存储的两种状态值，用于任务的启停控制。
     */
    public enum Status
    {
        /**
         * 任务正常运行状态，数据库值为 "0"
         */
        NORMAL("0"),
        /**
         * 任务暂停状态，数据库值为 "1"
         */
        PAUSE("1");

        /**
         * 状态值
         */
        private String value;

        /**
         * 构造函数
         * 
         * @param value 状态值字符串
         */
        private Status(String value)
        {
            this.value = value;
        }

        /**
         * 获取状态值
         * 
         * @return 状态值字符串
         */
        public String getValue()
        {
            return value;
        }
    }
}
