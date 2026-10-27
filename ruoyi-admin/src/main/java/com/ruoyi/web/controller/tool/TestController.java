package com.ruoyi.web.controller.tool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.utils.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Swagger / OpenAPI 演示用的简易用户 CRUD Controller
 *
 * <p>业务模块：工具 — OpenAPI 示例，路由前缀 {@code /test/user}。
 * 数据全部存储在类级静态 Map 中（进程内内存），重启即丢失。
 * 该控制器不含业务 Service 依赖，专用于 Swagger 文档与接口联调演示。</p>
 *
 * <p>注解说明：{@code @Tag} 为 Swagger 分组标签，{@code @Operation} 为每个接口补充 Swagger 摘要。</p>
 *
 * @author ruoyi
 */
@Tag(name = "用户信息管理")
@RestController
@RequestMapping("/test/user")
public class TestController extends BaseController
{
    /** 演示数据：以 userId 为键的内存 Map */
    private final static Map<Integer, UserEntity> users = new LinkedHashMap<Integer, UserEntity>();
    {
        users.put(1, new UserEntity(1, "admin", "admin123", "15888888888"));
        users.put(2, new UserEntity(2, "ry", "admin123", "15666666666"));
    }

    /**
     * 获取用户列表
     *
     * <p>url: GET /test/user/list</p>
     *
     * @param url 请求路径
     * @return R#ok(List<UserEntity>) 当前内存中全部用户
     */
    @Operation(summary = "获取用户列表")
    @GetMapping("/list")
    public R<List<UserEntity>> userList()
    {
        List<UserEntity> userList = new ArrayList<UserEntity>(users.values());
        return R.ok(userList);
    }

    /**
     * 获取单个用户详情
     *
     * <p>url: GET /test/user/{userId}</p>
     *
     * @param url 请求路径
     * @param userId 用户主键（路径变量）
     * @return R#ok(UserEntity) 找到的用户；R#fail("用户不存在") 否则
     */
    @Operation(summary = "获取用户详细")
    @GetMapping("/{userId}")
    public R<UserEntity> getUser(@PathVariable(name = "userId")
    Integer userId)
    {
        if (!users.isEmpty() && users.containsKey(userId))
        {
            return R.ok(users.get(userId));
        }
        else
        {
            return R.fail("用户不存在");
        }
    }

    /**
     * 新增用户（表单提交）
     *
     * <p>url: POST /test/user/save</p>
     *
     * <p>参数为表单字段绑定（非 JSON Body），字段名与 {@link UserEntity} 属性一致。</p>
     *
     * @param url 请求路径
     * @param user 表单绑定的用户对象
     * @return R#ok() 成功；R#fail("用户ID不能为空") 校验失败
     */
    @Operation(summary = "新增用户")
    @PostMapping("/save")
    public R<String> save(UserEntity user)
    {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUserId()))
        {
            return R.fail("用户ID不能为空");
        }
        users.put(user.getUserId(), user);
        return R.ok();
    }

    /**
     * 更新用户（JSON Body）
     *
     * <p>url: PUT /test/user/update</p>
     *
     * @param url 请求路径
     * @param user JSON 请求体，必须包含 userId
     * @return R#ok() 成功；R#fail("用户不存在") 或 "用户ID不能为空" 否则
     */
    @Operation(summary = "更新用户")
    @PutMapping("/update")
    public R<String> update(@RequestBody
    UserEntity user)
    {
        if (StringUtils.isNull(user) || StringUtils.isNull(user.getUserId()))
        {
            return R.fail("用户ID不能为空");
        }
        if (users.isEmpty() || !users.containsKey(user.getUserId()))
        {
            return R.fail("用户不存在");
        }
        users.remove(user.getUserId());
        users.put(user.getUserId(), user);
        return R.ok();
    }

    /**
     * 删除用户
     *
     * <p>url: DELETE /test/user/{userId}</p>
     *
     * @param url 请求路径
     * @param userId 用户主键（路径变量）
     * @return R#ok() 成功；R#fail("用户不存在") 否则
     */
    @Operation(summary = "删除用户信息")
    @DeleteMapping("/{userId}")
    public R<String> delete(@PathVariable(name = "userId")
    Integer userId)
    {
        if (!users.isEmpty() && users.containsKey(userId))
        {
            users.remove(userId);
            return R.ok();
        }
        else
        {
            return R.fail("用户不存在");
        }
    }
}

/**
 * 演示用用户实体（非数据库实体）
 *
 * <p>仅用于 {@link TestController} 的 OpenAPI 示例接口，不做持久化。</p>
 *
 * @author ruoyi
 */
@Schema(description = "用户实体")
class UserEntity
{
    /** 用户 ID */
    @Schema(title = "用户ID")
    private Integer userId;

    /** 用户名 */
    @Schema(title = "用户名称")
    private String username;

    /** 密码（演示用明文，真实系统禁止回传） */
    @Schema(title = "用户密码")
    private String password;

    /** 手机号 */
    @Schema(title = "用户手机")
    private String mobile;

    public UserEntity()
    {

    }

    public UserEntity(Integer userId, String username, String password, String mobile)
    {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.mobile = mobile;
    }

    public Integer getUserId()
    {
        return userId;
    }

    public void setUserId(Integer userId)
    {
        this.userId = userId;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }
}
