package com.ruoyi.quartz.util;

import java.text.ParseException;
import java.util.Date;
import org.quartz.CronExpression;

/**
 * Cron 表达式工具类
 *
 * <p>在项目中的角色：对 Quartz 原生 {@link CronExpression} 的薄封装，
 * 提供 Cron 表达式校验、错误消息获取、下一次执行时间计算三个基础能力。</p>
 *
 * <p>使用场景：</p>
 * <ul>
 *   <li>Controller 层在新增/修改任务时校验 Cron 合法性
 *       （{@link com.ruoyi.quartz.controller.SysJobController#add} / {@code edit}）；</li>
 *   <li>{@link SysJob#getNextValidTime} 通过本类动态计算"下一次执行时间"，
 *       用于前端展示，不持久化；</li>
 *   <li>{@link ScheduleUtils#createScheduleJob} 用 {@link #getNextExecution}
 *       判断任务是否过期（表达式不再产生未来触发点）。</li>
 * </ul>
 *
 * @author ruoyi
 */
public class CronUtils
{
    /**
     * 判断给定的 Cron 表达式是否合法。
     *
     * <p>委托 {@link CronExpression#isValidExpression} 完成实际校验。</p>
     *
     * @param cronExpression Quartz 风格的 Cron 表达式
     * @return {@code true} 表示语法合法；{@code false} 表示存在语法错误
     */
    public static boolean isValid(String cronExpression)
    {
        return CronExpression.isValidExpression(cronExpression);
    }

    /**
     * 获取 Cron 表达式的错误描述信息。
     *
     * <p>当表达式非法时，返回 {@link ParseException} 的消息；
     * 合法时返回 {@code null}。用于 Controller 层向前端返回友好提示。</p>
     *
     * @param cronExpression 待校验的 Cron 表达式
     * @return 错误描述字符串（非法时），或 {@code null}（合法时）
     */
    public static String getInvalidMessage(String cronExpression)
    {
        try
        {
            new CronExpression(cronExpression);
            return null;
        }
        catch (ParseException pe)
        {
            return pe.getMessage();
        }
    }

    /**
     * 根据给定的 Cron 表达式计算下一次执行时间。
     *
     * <p>以当前系统时间（{@code new Date(System.currentTimeMillis())}）
     * 为基准，返回该 Cron 表达式下一次的触发时间点。</p>
     *
     * <p>若表达式非法，直接将 {@link ParseException} 包装为
     * {@link IllegalArgumentException} 抛出——因为调用方通常假设参数已通过
     * {@link #isValid} 校验。</p>
     *
     * @param cronExpression Quartz 风格的 Cron 表达式
     * @return 下一次执行时间；若表达式合法但不再有未来触发点则返回 {@code null}
     * @throws IllegalArgumentException 当 Cron 表达式语法非法时抛出
     */
    public static Date getNextExecution(String cronExpression)
    {
        try
        {
            CronExpression cron = new CronExpression(cronExpression);
            return cron.getNextValidTimeAfter(new Date(System.currentTimeMillis()));
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException(e.getMessage());
        }
    }
}
