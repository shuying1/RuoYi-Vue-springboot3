package com.ruoyi.framework.web.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.user.CaptchaException;
import com.ruoyi.common.exception.user.CaptchaExpireException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.MessageUtils;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.manager.AsyncManager;
import com.ruoyi.framework.manager.factory.AsyncFactory;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysUserService;

/**
 * 用户注册服务 —— 自助注册入口
 *
 * <p>设计意图：处理前端 RegisterBody 的注册请求，完成参数校验（用户名/密码长度、验证码、唯一性检查）
 * → BCrypt 密码加密 → 写入 sys_user 表 → 异步记录注册日志。</p>
 *
 * <p>注册前提：系统配置 {@code sys.account.registerUser} 需开启（默认开启）。</p>
 *
 * <p>执行流程：</p>
 * <ol>
 *   <li>校验验证码（若开启）—— 与 {@link SysLoginService#validateCaptcha} 逻辑一致</li>
 *   <li>参数非空校验 + 长度范围校验（用户名 2~20 字符，密码 5~20 字符）</li>
 *   <li>用户名唯一性检查 —— 调用 ISysUserService.checkUserNameUnique</li>
 *   <li>构造 SysUser：nickName = username（默认）、pwdUpdateDate = now、password = BCrypt 加密值</li>
 *   <li>写入数据库 —— ISysUserService.registerUser</li>
 *   <li>异步记录注册日志到 sys_logininfor</li>
 * </ol>
 *
 * <p>关键依赖：</p>
 * <ul>
 *   <li>{@link ISysUserService} —— 用户唯一性检查与注册写入</li>
 *   <li>{@link ISysConfigService} —— 读取验证码开关</li>
 *   <li>{@link RedisCache} —— 验证码校验</li>
 *   <li>{@link SecurityUtils#encryptPassword} —— BCrypt 密码加密</li>
 * </ul>
 *
 * @author ruoyi
 * @see RegisterBody 注册请求体（前端提交的 username/password/code/uuid）
 * @see SysLoginService 登录服务（共用验证码校验逻辑）
 */
@Component
public class SysRegisterService
{
    /** 用户数据库操作服务 */
    @Autowired
    private ISysUserService userService;

    /** 系统配置读取服务 */
    @Autowired
    private ISysConfigService configService;

    /** Redis 缓存，用于验证码校验 */
    @Autowired
    private RedisCache redisCache;

    /**
     * 执行用户注册。
     *
     * <p>校验失败时返回错误消息字符串（非抛异常），
     * Controller 层根据返回值判断注册是否成功；
     * 注册成功时返回空字符串 {@code ""}。</p>
     *
     * @param registerBody 前端提交的注册表单（包含 username、password、code、uuid）
     * @return 错误消息；空字符串表示注册成功
     */
    public String register(RegisterBody registerBody)
    {
        String msg = "", username = registerBody.getUsername(), password = registerBody.getPassword();
        SysUser sysUser = new SysUser();
        sysUser.setUserName(username);

        // 验证码开关
        boolean captchaEnabled = configService.selectCaptchaEnabled();
        if (captchaEnabled)
        {
            validateCaptcha(username, registerBody.getCode(), registerBody.getUuid());
        }

        if (StringUtils.isEmpty(username))
        {
            msg = "用户名不能为空";
        }
        else if (StringUtils.isEmpty(password))
        {
            msg = "用户密码不能为空";
        }
        else if (username.length() < UserConstants.USERNAME_MIN_LENGTH
                || username.length() > UserConstants.USERNAME_MAX_LENGTH)
        {
            msg = "账户长度必须在2到20个字符之间";
        }
        else if (password.length() < UserConstants.PASSWORD_MIN_LENGTH
                || password.length() > UserConstants.PASSWORD_MAX_LENGTH)
        {
            msg = "密码长度必须在5到20个字符之间";
        }
        else if (!userService.checkUserNameUnique(sysUser))
        {
            msg = "保存用户'" + username + "'失败，注册账号已存在";
        }
        else
        {
            sysUser.setNickName(username);
            sysUser.setPwdUpdateDate(DateUtils.getNowDate());
            sysUser.setPassword(SecurityUtils.encryptPassword(password));
            boolean regFlag = userService.registerUser(sysUser);
            if (!regFlag)
            {
                msg = "注册失败,请联系系统管理人员";
            }
            else
            {
                AsyncManager.me().execute(AsyncFactory.recordLogininfor(username, Constants.REGISTER, MessageUtils.message("user.register.success")));
            }
        }
        return msg;
    }

    /**
     * 校验注册验证码（逻辑与 {@link SysLoginService#validateCaptcha} 一致）。
     *
     * <p>从 Redis 读取验证码（Key = CAPTCHA_CODE_KEY + uuid），
     * 用后即删，防止重放攻击。</p>
     *
     * @param username 用户名（失败日志用）
     * @param code 前端提交的验证码
     * @param uuid 验证码唯一标识
     * @throws CaptchaExpireException 验证码过期（Redis 中不存在）
     * @throws CaptchaException 验证码不匹配
     */
    public void validateCaptcha(String username, String code, String uuid)
    {
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisCache.getCacheObject(verifyKey);
        redisCache.deleteObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaExpireException();
        }
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException();
        }
    }
}
