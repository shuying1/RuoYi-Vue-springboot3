package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.file.FileUploadUtils;
import com.ruoyi.common.utils.file.FileUtils;
import com.ruoyi.common.utils.file.MimeTypeUtils;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 个人中心接口控制器
 *
 * <p>业务模块：系统管理 — 个人中心，路由前缀 {@code /system/user/profile}，
 * 依赖 Service：{@link ISysUserService}、{@link TokenService}、{@link RuoYiConfig}。
 * 与 {@link SysUserController} 不同，本控制器操作对象固定为"当前登录用户"，
 * 不涉及数据范围校验，也不涉及其他用户的管理。所有写操作会同步刷新 Redis 中的 token 缓存。</p>
 *
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET    /system/user/profile}        — 获取当前用户个人信息（含角色组、岗位组）</li>
 *   <li>{@code PUT    /system/user/profile}        — 修改昵称/邮箱/手机号/性别</li>
 *   <li>{@code PUT    /system/user/profile/updatePwd}  — 修改登录密码（需校验旧密码）</li>
 *   <li>{@code POST   /system/user/profile/avatar}  — 上传头像（multipart/form-data，字段名 avatarfile）</li>
 * </ul>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/system/user/profile")
public class SysProfileController extends BaseController
{
    /** 用户 Service：用于唯一性校验与个人资料落库 */
    @Autowired
    private ISysUserService userService;

    /** token 管理服务：用于写操作后刷新登录缓存 */
    @Autowired
    private TokenService tokenService;

    /**
     * 获取当前用户个人信息
     *
     * <p>url: GET /system/user/profile</p>
     *
     * <p>响应 data 结构：</p>
     * <pre>
     * {
     *   "user":       SysUser,            // 当前用户实体（不含密码/盐）
     *   "roleGroup":  "超级管理员、普通角色", // 角色名称组合，逗号分隔
     *   "postGroup":  "CEO、CTO"           // 岗位名称组合，逗号分隔
     * }
     * </pre>
     *
     * @param url 请求路径
     * @return AjaxResult
     */
    @GetMapping
    public AjaxResult profile()
    {
        LoginUser loginUser = getLoginUser();
        SysUser user = loginUser.getUser();
        AjaxResult ajax = AjaxResult.success(user);
        ajax.put("roleGroup", userService.selectUserRoleGroup(loginUser.getUsername()));
        ajax.put("postGroup", userService.selectUserPostGroup(loginUser.getUsername()));
        return ajax;
    }

    /**
     * 修改当前用户基本资料（昵称、邮箱、手机号、性别）
     *
     * <p>url: PUT /system/user/profile</p>
     *
     * <p>会校验手机号与邮箱唯一性；成功后同步刷新 token 缓存。</p>
     *
     * @param url 请求路径
     * @param user 请求体（JSON），含 nickName、email、phonenumber、sex 等字段
     * @return AjaxResult
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult updateProfile(@RequestBody SysUser user)
    {
        LoginUser loginUser = getLoginUser();
        SysUser currentUser = loginUser.getUser();
        currentUser.setNickName(user.getNickName());
        currentUser.setEmail(user.getEmail());
        currentUser.setPhonenumber(user.getPhonenumber());
        currentUser.setSex(user.getSex());
        if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，手机号码已存在");
        }
        if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(currentUser))
        {
            return error("修改用户'" + loginUser.getUsername() + "'失败，邮箱账号已存在");
        }
        if (userService.updateUserProfile(currentUser) > 0)
        {
            // 更新缓存用户信息
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改个人信息异常，请联系管理员");
    }

    /**
     * 修改当前用户登录密码
     *
     * <p>url: PUT /system/user/profile/updatePwd</p>
     *
     * <p>请求体（JSON）：{@code {"oldPassword": "...", "newPassword": "..."}}。
     * 会校验旧密码正确性、新旧密码不能相同；成功后 BCrypt 加密入库，
     * 并刷新 token 缓存中的密码与密码最后更新时间。</p>
     *
     * @param url 请求路径
     * @param params 含 oldPassword 与 newPassword 的 Map
     * @return AjaxResult
     */
    @Log(title = "个人信息", businessType = BusinessType.UPDATE)
    @PutMapping("/updatePwd")
    public AjaxResult updatePwd(@RequestBody Map<String, String> params)
    {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        LoginUser loginUser = getLoginUser();
        Long userId = loginUser.getUserId();
        SysUser user = userService.selectUserById(userId);
        String password = user.getPassword();
        if (!SecurityUtils.matchesPassword(oldPassword, password))
        {
            return error("修改密码失败，旧密码错误");
        }
        if (SecurityUtils.matchesPassword(newPassword, password))
        {
            return error("新密码不能与旧密码相同");
        }
        newPassword = SecurityUtils.encryptPassword(newPassword);
        if (userService.resetUserPwd(userId, newPassword) > 0)
        {
            // 更新缓存用户密码&密码最后更新时间
            loginUser.getUser().setPwdUpdateDate(DateUtils.getNowDate());
            loginUser.getUser().setPassword(newPassword);
            tokenService.setLoginUser(loginUser);
            return success();
        }
        return error("修改密码异常，请联系管理员");
    }

    /**
     * 上传当前用户头像
     *
     * <p>url: POST /system/user/profile/avatar</p>
     *
     * <p>Content-Type: multipart/form-data，字段名 {@code avatarfile}。
     * 成功后会删除旧头像文件、刷新 token 缓存；返回新头像访问路径。</p>
     *
     * @param url 请求路径
     * @param file 上传的图片文件
     * @return AjaxResult#success({imgUrl: "..."})
     * @throws Exception 文件读写异常
     */
    @Log(title = "用户头像", businessType = BusinessType.UPDATE)
    @PostMapping("/avatar")
    public AjaxResult avatar(@RequestParam("avatarfile") MultipartFile file) throws Exception
    {
        if (!file.isEmpty())
        {
            LoginUser loginUser = getLoginUser();
            String avatar = FileUploadUtils.upload(RuoYiConfig.getAvatarPath(), file, MimeTypeUtils.IMAGE_EXTENSION, true);
            if (userService.updateUserAvatar(loginUser.getUserId(), avatar))
            {
                String oldAvatar = loginUser.getUser().getAvatar();
                if (StringUtils.isNotEmpty(oldAvatar))
                {
                    FileUtils.deleteFile(RuoYiConfig.getProfile() + FileUtils.stripPrefix(oldAvatar));
                }
                AjaxResult ajax = AjaxResult.success();
                ajax.put("imgUrl", avatar);
                // 更新缓存用户头像
                loginUser.getUser().setAvatar(avatar);
                tokenService.setLoginUser(loginUser);
                return ajax;
            }
        }
        return error("上传图片异常，请联系管理员");
    }
}
