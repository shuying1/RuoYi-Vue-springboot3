package com.ruoyi.framework.aspectj;

import java.util.ArrayList;
import java.util.List;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import com.ruoyi.common.annotation.DataScope;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.domain.entity.SysRole;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.text.Convert;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.security.context.PermissionContextHolder;

/**
 * 数据权限过滤切面处理
 *
 * <p>设计意图：通过 AOP 拦截所有标注了 {@link DataScope} 注解的 Service 方法，
 * 自动根据当前登录用户的角色和数据权限范围，在 SQL 查询参数中注入额外的过滤条件，
 * 从而实现行级数据权限控制（Row-Level Security）。</p>
 *
 * <p>拦截流程：</p>
 * <ol>
 *   <li>前置通知 {@code doBefore} 触发，先清空参数中已有的 dataScope 防止 SQL 注入</li>
 *   <li>获取当前登录用户，超级管理员跳过过滤</li>
 *   <li>遍历用户所有角色，根据角色的数据权限范围（全部/自定义/本部门/本部门及以下/仅本人）拼接 SQL 片段</li>
 *   <li>将最终生成的 SQL 条件（如 " AND (d.dept_id = 100 OR ...)"）写入 {@link BaseEntity#getParams()} 中</li>
 *   <li>MyBatis XML 中的 ${params.dataScope} 将该条件拼接到 SELECT 语句中</li>
 * </ol>
 *
 * <p>支持五种数据权限范围：</p>
 * <ul>
 *   <li>全部数据（DATA_SCOPE_ALL）：直接返回，不附加任何过滤条件</li>
 *   <li>自定义数据（DATA_SCOPE_CUSTOM）：查询 sys_role_dept 中间表获取角色关联的部门</li>
 *   <li>本部门数据（DATA_SCOPE_DEPT）：过滤条件为 dept_id = 当前用户部门 ID</li>
 *   <li>本部门及以下（DATA_SCOPE_DEPT_AND_CHILD）：使用 find_in_set 查询部门树中当前部门的所有子部门</li>
 *   <li>仅本人数据（DATA_SCOPE_SELF）：过滤条件为 user_id = 当前用户 ID</li>
 * </ul>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link SecurityUtils} — 获取当前登录用户</li>
 *   <li>{@link PermissionContextHolder} — 从注解或上下文获取权限字符</li>
 * </ul>
 *
 * @author ruoyi
 */
@Aspect
@Component
public class DataScopeAspect
{
    /**
     * 数据权限过滤在 BaseEntity.params Map 中对应的 Key，
     * MyBatis XML 中通过 ${params.dataScope} 引用该值拼接到 SQL 语句。
     */
    public static final String DATA_SCOPE = "dataScope";

    /**
     * 前置通知，拦截所有标注了 {@link DataScope} 注解的方法。
     *
     * <p>执行顺序：先清空已有过滤条件（防止 SQL 注入），再根据当前用户权限重新拼接。</p>
     *
     * @param point 切点对象，可获取目标方法参数
     * @param controllerDataScope 方法或类上标注的 {@link DataScope} 注解实例
     * @throws Throwable 被拦截方法抛出的异常
     */
    @Before("@annotation(controllerDataScope)")
    public void doBefore(JoinPoint point, DataScope controllerDataScope) throws Throwable
    {
        clearDataScope(point);
        handleDataScope(point, controllerDataScope);
    }

    /**
     * 核心数据权限处理逻辑（非超级管理员时进入）。
     *
     * <p>从 {@link SecurityUtils} 获取 {@link LoginUser}，取出 {@link SysUser}，
     * 然后调用 {@link #dataScopeFilter} 进行实际的 SQL 条件拼接。</p>
     *
     * @param joinPoint AOP 切点
     * @param controllerDataScope DataScope 注解实例
     */
    protected void handleDataScope(final JoinPoint joinPoint, DataScope controllerDataScope)
    {
        // 获取当前的用户
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isNotNull(loginUser))
        {
            SysUser currentUser = loginUser.getUser();
            // 如果是超级管理员，则不过滤数据（超级管理员拥有全部数据权限，直接放行）
            if (StringUtils.isNotNull(currentUser) && !currentUser.isAdmin())
            {
                // 优先使用注解中显式声明的 permission，否则回退到 PermissionContextHolder 上下文
                String permission = StringUtils.defaultIfEmpty(controllerDataScope.permission(), PermissionContextHolder.getContext());
                dataScopeFilter(joinPoint, currentUser, controllerDataScope.userAlias(), controllerDataScope.deptAlias(), controllerDataScope.userField(), controllerDataScope.deptField(), permission);
            }
        }
    }

    /**
     * 数据范围过滤 —— 根据用户角色的数据权限范围拼接 SQL 条件。
     *
     * <p>处理逻辑：</p>
     * <ol>
     *   <li>先收集所有启用中的自定义数据权限角色（scopeCustomIds），用于后续批量 IN 查询</li>
     *   <li>遍历每个角色，根据 dataScope 字段类型对应不同的 SQL 片段：
     *       <ul>
     *         <li>全部数据 → 清空 sqlString 并 break（最高优先级）</li>
     *         <li>自定义数据 → 用子查询 sys_role_dept 表</li>
     *         <li>本部门 → dept_id = ?</li>
     *         <li>本部门及以下 → dept_id = ? OR find_in_set(?, ancestors)</li>
     *         <li>仅本人 → user_id = ?（若无 userAlias，则令 dept_id = 0，即不返回任何数据）</li>
     *       </ul>
     *   </li>
     *   <li>去重：已处理过的数据范围（conditions 中存在）或角色已禁用则跳过</li>
     *   <li>所有角色都不包含指定 permission 时，追加 "dept_id = 0" 防止查全表</li>
     *   <li>将最终字符串去除开头 " OR " 后放入 BaseEntity.params[DATA_SCOPE]</li>
     * </ol>
     *
     * @param joinPoint 切点（通过 joinPoint.getArgs()[0] 获取第一个参数，应为 BaseEntity 子类）
     * @param user 当前登录用户（非超级管理员）
     * @param userAlias 用户表别名，用于仅本人权限时拼接 user_id 条件；可为空
     * @param deptAlias 部门表别名，必须指定
     * @param userField 用户 ID 字段名，默认 user_id
     * @param deptField 部门 ID 字段名，默认 dept_id
     * @param permission 权限字符，用于过滤不相关的角色；为空时不做角色权限匹配
     */
    public static void dataScopeFilter(JoinPoint joinPoint, SysUser user, String userAlias, String deptAlias, String userField, String deptField, String permission)
    {
        StringBuilder sqlString = new StringBuilder();
        List<String> conditions = new ArrayList<String>();
        // 收集所有启用中的自定义数据权限角色 ID，用于批量 IN 查询优化
        List<String> scopeCustomIds = new ArrayList<String>();
        user.getRoles().forEach(role -> {
            if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(role.getDataScope()) && StringUtils.equals(role.getStatus(), UserConstants.ROLE_NORMAL) && (StringUtils.isEmpty(permission) || StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission))))
            {
                scopeCustomIds.add(Convert.toStr(role.getRoleId()));
            }
        });

        // 遍历用户所有角色，逐个处理数据权限范围
        for (SysRole role : user.getRoles())
        {
            String dataScope = role.getDataScope();
            // 已处理过该数据范围（去重）或角色已被禁用，跳过
            if (conditions.contains(dataScope) || StringUtils.equals(role.getStatus(), UserConstants.ROLE_DISABLE))
            {
                continue;
            }
            // 指定了 permission 但该角色的权限列表中不包含，则跳过该角色的数据范围
            if (StringUtils.isNotEmpty(permission) && !StringUtils.containsAny(role.getPermissions(), Convert.toStrArray(permission)))
            {
                continue;
            }
            if (Constants.Dept.DATA_SCOPE_ALL.equals(dataScope))
            {
                // 全部数据权限最高优先级，直接清空并跳出循环
                sqlString = new StringBuilder();
                conditions.add(dataScope);
                break;
            }
            else if (Constants.Dept.DATA_SCOPE_CUSTOM.equals(dataScope))
            {
                // 自定义数据权限：查询 sys_role_dept 中间表中该角色关联的部门
                if (scopeCustomIds.size() > 1)
                {
                    // 多个自定义数据权限角色使用 IN 子查询，避免多次拼接
                    sqlString.append(StringUtils.format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id in ({}) ) ", deptAlias, deptField, String.join(",", scopeCustomIds)));
                }
                else
                {
                    sqlString.append(StringUtils.format(" OR {}.{} IN ( SELECT dept_id FROM sys_role_dept WHERE role_id = {} ) ", deptAlias, deptField, role.getRoleId()));
                }
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT.equals(dataScope))
            {
                // 本部门数据：dept_id = 当前用户的部门 ID
                sqlString.append(StringUtils.format(" OR {}.{} = {} ", deptAlias, deptField, user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_DEPT_AND_CHILD.equals(dataScope))
            {
                // 本部门及以下：使用 find_in_set 查询部门树中 ancestors 字段包含当前部门 ID 的所有子部门
                sqlString.append(StringUtils.format(" OR {}.{} IN ( SELECT dept_id FROM sys_dept WHERE dept_id = {} or find_in_set( {} , ancestors ) )", deptAlias, deptField, user.getDeptId(), user.getDeptId()));
            }
            else if (Constants.Dept.DATA_SCOPE_SELF.equals(dataScope))
            {
                // 仅本人数据：user_id = 当前用户 ID
                if (StringUtils.isNotBlank(userAlias))
                {
                    sqlString.append(StringUtils.format(" OR {}.{} = {} ", userAlias, userField, user.getUserId()));
                }
                else
                {
                    // 数据权限为仅本人且没有 userAlias 别名，SQL 拼接 dept_id = 0 使查询不返回任何数据
                    sqlString.append(StringUtils.format(" OR {}.{} = 0 ", deptAlias, deptField));
                }
            }
            conditions.add(dataScope);
        }

        // 角色都不包含传递过来的权限字符，这个时候 sqlString 也会为空，所以要限制一下，不查询任何数据
        if (StringUtils.isEmpty(conditions))
        {
            sqlString.append(StringUtils.format(" OR {}.{} = 0 ", deptAlias, deptField));
        }

        // 将最终拼接的 SQL 条件写入 BaseEntity.params 中，供 MyBatis XML 使用 ${params.dataScope} 引用
        if (StringUtils.isNotBlank(sqlString.toString()))
        {
            Object params = joinPoint.getArgs()[0];
            if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
            {
                BaseEntity baseEntity = (BaseEntity) params;
                // substring(4) 去掉开头多余的 " OR "，最终形态如 " AND (d.dept_id = 1 OR d.dept_id IN (...))"
                baseEntity.getParams().put(DATA_SCOPE, " AND (" + sqlString.substring(4) + ")");
            }
        }
    }

    /**
     * 拼接权限 SQL 前先清空 params.dataScope 参数防止 SQL 注入。
     *
     * <p>调用时机：在 {@link #handleDataScope} 之前执行，
     * 避免请求参数中带有 dataScope 键值被直接拼接到 SQL 造成注入风险。</p>
     *
     * @param joinPoint 切点（取第一个参数作为 BaseEntity）
     */
    private void clearDataScope(final JoinPoint joinPoint)
    {
        Object params = joinPoint.getArgs()[0];
        if (StringUtils.isNotNull(params) && params instanceof BaseEntity)
        {
            BaseEntity baseEntity = (BaseEntity) params;
            baseEntity.getParams().put(DATA_SCOPE, "");
        }
    }
}
