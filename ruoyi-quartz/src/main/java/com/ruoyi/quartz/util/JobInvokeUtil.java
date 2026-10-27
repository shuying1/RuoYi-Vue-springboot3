package com.ruoyi.quartz.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.spring.SpringUtils;
import com.ruoyi.quartz.domain.SysJob;

/**
 * 定时任务反射执行工具类
 *
 * <p>在项目中的角色：将用户配置的 invokeTarget 字符串（如
 * {@code "ryTask.ryMultipleParams('hello', true, 100L, 3.14D, 99)"} 或
 * {@code "com.ruoyi.quartz.task.RyTask.ryParams('abc')"}）解析并反射执行。</p>
 *
 * <p>关键设计：</p>
 * <ul>
 *   <li><b>两种调用模式</b>：</li>
 *     <ul>
 *       <li>Spring Bean 模式：beanName.methodName(params) — 通过
 *           {@code SpringUtils.getBean(beanName)} 获取 Bean 实例；</li>
 *       <li>全限定类名模式：全限定类名.methodName(params) — 通过
 *           {@code Class.forName(beanName).getDeclaredConstructor().newInstance()}
 *           反射创建实例（要求类必须有默认构造器）。</li>
 *     </ul>
 *   <li><b>安全边界</b>：本类本身执行反射，不做任何安全校验。
 *       安全前置全部在 {@link ScheduleUtils#whiteList}（调度前）
 *       和 {@link SysJobController#add/edit}（新增修改时）完成——
 *       只有通过白名单的 invokeTarget 才会到达这里。</li>
 *   <li><b>参数类型推断</b>：{@link #getMethodParams} 解析括号内的参数字符串，
 *       根据字面量特征推断类型：带引号 → String，true/false → Boolean，
 *       以 L 结尾 → Long，以 D 结尾 → Double，其余 → Integer。</li>
 *   <li><b>逗号切分</b>：使用正则 {@code "," (?= ([^"'*"'"']*["'][^"'*"'"']*["'])*[^"'*"'"']*$)}
 *       保证引号内的逗号不会被误切，如 {@code "a,b", 1} 会被正确分为两个参数。</li>
 * </ul>
 *
 * @author ruoyi
 */
public class JobInvokeUtil
{
    /**
     * 根据 {@link SysJob#invokeTarget} 反射执行目标方法。
     *
     * <p>完整流程：</p>
     * <ol>
     *   <li>{@link #getBeanName} 提取 bean 或全限定类名；</li>
     *   <li>{@link #getMethodName} 提取方法名；</li>
     *   <li>{@link #getMethodParams} 解析参数字符串为 List&lt;Object[]&gt;
     *       （每项为 [值, 类型]）；</li>
     *   <li>{@link #isValidClassName} 判断是 Spring Bean 还是全限定类名；</li>
     *   <li>分别获取 Bean 实例或反射创建实例；</li>
     *   <li>调 {@link #invokeMethod(Object, String, List)} 完成实际反射调用。</li>
     * </ol>
     *
     * @param sysJob 任务配置，其 {@code invokeTarget} 字段是唯一输入
     * @throws Exception 反射过程中的任何异常（类不存在、方法不存在、实例化失败、业务异常等）
     */
    public static void invokeMethod(SysJob sysJob) throws Exception
    {
        String invokeTarget = sysJob.getInvokeTarget();
        String beanName = getBeanName(invokeTarget);
        String methodName = getMethodName(invokeTarget);
        List<Object[]> methodParams = getMethodParams(invokeTarget);

        if (!isValidClassName(beanName))
        {
            Object bean = SpringUtils.getBean(beanName);
            invokeMethod(bean, methodName, methodParams);
        }
        else
        {
            Object bean = Class.forName(beanName).getDeclaredConstructor().newInstance();
            invokeMethod(bean, methodName, methodParams);
        }
    }

    /**
     * 真正执行反射调用的私有方法。
     *
     * <p>先根据 methodParams 是否为空判断调用无参方法还是带参方法：</p>
     * <ul>
     *   <li>带参：用 {@link #getMethodParamsType} 取出参数类型数组，
     *       通过 {@code bean.getClass().getMethod(methodName, types)} 找到公开方法；</li>
     *   <li>无参：直接 {@code bean.getClass().getMethod(methodName)}。</li>
     * </ul>
     * <p>注意：这里用 {@code getMethod} 而非 {@code getDeclaredMethod}，
     * 意味着只能调用 public 方法——这是一个隐含的安全设计，
     * 防止通过定时任务调用 private/protected 方法。</p>
     *
     * @param bean         目标对象（Spring Bean 或反射创建的实例）
     * @param methodName   目标方法名
     * @param methodParams 解析后的参数列表（每项 [值, 类型]），为 null 或空时表示无参调用
     * @throws NoSuchMethodException     目标方法不存在
     * @throws SecurityException         安全管理器拒绝反射访问
     * @throws IllegalAccessException    方法不可访问
     * @throws IllegalArgumentException  参数类型不匹配
     * @throws InvocationTargetException 方法执行时抛出的异常（被包装在此异常内）
     */
    private static void invokeMethod(Object bean, String methodName, List<Object[]> methodParams)
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
            InvocationTargetException
    {
        if (StringUtils.isNotNull(methodParams) && methodParams.size() > 0)
        {
            Method method = bean.getClass().getMethod(methodName, getMethodParamsType(methodParams));
            method.invoke(bean, getMethodParamsValue(methodParams));
        }
        else
        {
            Method method = bean.getClass().getMethod(methodName);
            method.invoke(bean);
        }
    }

    /**
     * 判断给定名称是否为"全限定类名"写法。
     *
     * <p>判断标准：字符串中包含的 {@code '.'} 数量大于 1。
     * 这样可以区分 {@code "ryTask"}（Spring Bean 名，0 个点）、
     * {@code "ryTask.method"}（Bean.method，1 个点）、
     * {@code "com.ruoyi.RyTask"}（全限定类名，2 个点以上）三种情况。</p>
     *
     * @param invokeTarget 可能是 Bean 名也可能是全限定类名
     * @return {@code true} 表示是全限定类名（含 2+ 个点）；{@code false} 表示是 Spring Bean 名
     */
    public static boolean isValidClassName(String invokeTarget)
    {
        return StringUtils.countMatches(invokeTarget, ".") > 1;
    }

    /**
     * 从 invokeTarget 中提取 Bean 名称或全限定类名。
     *
     * <p>示例：</p>
     * <ul>
     *   <li>{@code "ryTask.ryParams('abc')"} → {@code "ryTask"}</li>
     *   <li>{@code "com.ruoyi.RyTask.ryParams('abc')"} → {@code "com.ruoyi.RyTask"}</li>
     * </ul>
     *
     * @param invokeTarget 完整调用目标字符串，格式 {@code name.method(params)}
     * @return 去掉方法名和括号后的前半部分
     */
    public static String getBeanName(String invokeTarget)
    {
        String beanName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringBeforeLast(beanName, ".");
    }

    /**
     * 从 invokeTarget 中提取方法名。
     *
     * <p>示例：</p>
     * <ul>
     *   <li>{@code "ryTask.ryParams('abc')"} → {@code "ryParams"}</li>
     * </ul>
     *
     * @param invokeTarget 完整调用目标字符串
     * @return 方法名
     */
    public static String getMethodName(String invokeTarget)
    {
        String methodName = StringUtils.substringBefore(invokeTarget, "(");
        return StringUtils.substringAfterLast(methodName, ".");
    }

    /**
     * 从 invokeTarget 的括号内解析参数列表，并推断每个参数的 Java 类型。
     *
     * <p>类型推断规则（按优先级）：</p>
     * <ol>
     *   <li>以 {@code '} 或 {@code "} 开头 → String（去掉首尾引号）；</li>
     *   <li>忽略大小写等于 {@code true} / {@code false} → Boolean；</li>
     *   <li>以大写 {@code L} 结尾 → Long（去掉 L 后解析）；</li>
     *   <li>以大写 {@code D} 结尾 → Double（去掉 D 后解析）；</li>
     *   <li>其他 → Integer。</li>
     * </ol>
     * <p>返回值中每项是一个二元数组：{@code [实际值, 对应的 Class 类型]}。</p>
     *
     * @param invokeTarget 完整调用目标字符串
     * @return 参数列表，每项为 {@code [值, Class]}；无参数时返回 null
     */
    public static List<Object[]> getMethodParams(String invokeTarget)
    {
        String methodStr = StringUtils.substringBetweenLast(invokeTarget, "(", ")");
        if (StringUtils.isEmpty(methodStr))
        {
            return null;
        }
        String[] methodParams = methodStr.split(",(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)");
        List<Object[]> classs = new LinkedList<>();
        for (int i = 0; i < methodParams.length; i++)
        {
            String str = StringUtils.trimToEmpty(methodParams[i]);
            // String字符串类型，以'或"开头
            if (StringUtils.startsWithAny(str, "'", "\""))
            {
                classs.add(new Object[] { StringUtils.substring(str, 1, str.length() - 1), String.class });
            }
            // boolean布尔类型，等于true或者false
            else if ("true".equalsIgnoreCase(str) || "false".equalsIgnoreCase(str))
            {
                classs.add(new Object[] { Boolean.valueOf(str), Boolean.class });
            }
            // long长整形，以L结尾
            else if (StringUtils.endsWith(str, "L"))
            {
                classs.add(new Object[] { Long.valueOf(StringUtils.substring(str, 0, str.length() - 1)), Long.class });
            }
            // double浮点类型，以D结尾
            else if (StringUtils.endsWith(str, "D"))
            {
                classs.add(new Object[] { Double.valueOf(StringUtils.substring(str, 0, str.length() - 1)), Double.class });
            }
            // 其他类型归类为整形
            else
            {
                classs.add(new Object[] { Integer.valueOf(str), Integer.class });
            }
        }
        return classs;
    }

    /**
     * 从参数列表中提取所有参数的类型数组。
     *
     * @param methodParams 参数列表（每项为 [值, Class]）
     * @return 与 methodParams 顺序对应的 Class 数组，可直接传给
     *         {@link Method#getMethod} 查找目标方法签名
     */
    public static Class<?>[] getMethodParamsType(List<Object[]> methodParams)
    {
        Class<?>[] classs = new Class<?>[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams)
        {
            classs[index] = (Class<?>) os[1];
            index++;
        }
        return classs;
    }

    /**
     * 从参数列表中提取所有参数的实际值数组。
     *
     * @param methodParams 参数列表（每项为 [值, Class]）
     * @return 与 methodParams 顺序对应的 Object 数组，可直接传给
     *         {@link Method#invoke} 执行反射调用
     */
    public static Object[] getMethodParamsValue(List<Object[]> methodParams)
    {
        Object[] classs = new Object[methodParams.size()];
        int index = 0;
        for (Object[] os : methodParams)
        {
            classs[index] = (Object) os[0];
            index++;
        }
        return classs;
    }
}
