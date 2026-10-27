package com.ruoyi.quartz.task;

import org.springframework.stereotype.Component;
import com.ruoyi.common.utils.StringUtils;

/**
 * 定时任务调度测试 Bean
 *
 * <p>在项目中的角色：作为项目内置的 Spring Bean 示例（bean 名称 {@code "ryTask"}），
 * 供用户在"定时任务"管理界面填写 invokeTarget 时参考测试。</p>
 *
 * <p>调用示例：</p>
 * <ul>
 *   <li>{@code ryTask.ryNoParams()} — 无参方法；</li>
 *   <li>{@code ryTask.ryParams('Hello')} — 单 String 参数；</li>
 *   <li>{@code ryTask.ryMultipleParams('Hi', true, 100L, 3.14D, 99)} — 多参组合。</li>
 * </ul>
 *
 * @author ruoyi
 */
@Component("ryTask")
public class RyTask
{
    /**
     * 多类型参数测试方法——同时演示 String/Boolean/Long/Double/Integer 五种入参。
     *
     * @param s 字符串参数
     * @param b 布尔参数
     * @param l 长整型参数
     * @param d 浮点参数
     * @param i 整型参数
     */
    public void ryMultipleParams(String s, Boolean b, Long l, Double d, Integer i)
    {
        System.out.println(StringUtils.format("执行多参方法： 字符串类型{}，布尔类型{}，长整型{}，浮点型{}，整形{}", s, b, l, d, i));
    }

    /**
     * 单 String 参数测试方法。
     *
     * @param params 任意字符串参数
     */
    public void ryParams(String params)
    {
        System.out.println("执行有参方法：" + params);
    }

    /**
     * 无参测试方法——最简单的定时任务调用示例。
     */
    public void ryNoParams()
    {
        System.out.println("执行无参方法");
    }
}
