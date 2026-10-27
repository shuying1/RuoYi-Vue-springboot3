package com.ruoyi.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.RegisterBody;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.framework.web.service.SysRegisterService;
import com.ruoyi.system.service.ISysConfigService;

/**
 * 用户注册接口控制器
 * 
 * <p>对外开放的注册接口（匿名可访问），路由为 {@code POST /register}。
 * 是否允许注册由系统参数 {@code sys.account.registerUser} 控制（默认关闭）。</p>
 * 
 * @author ruoyi
 */
@RestController
public class SysRegisterController extends BaseController
{
    @Autowired
    private SysRegisterService registerService;

    /** 系统参数配置服务：用于读取注册开关 */
    @Autowired
    private ISysConfigService configService;

    /**
     * 用户自助注册
     * 
     * <p>POST /register</p>
     * 
     * <p>请求体（JSON）：</p>
     * <pre>
     * {
     *   "username": "zhangsan",
     *   "password": "123456",
     *   "code":     "a1b2",         // 验证码
     *   "uuid":     "xxxx-xxxx"     // 验证码 uuid
     * }
     * </pre>
     * 
     * @param user 注册请求体
     * @return AjaxResult 成功返回 success()，失败返回 error(提示信息)
     */
    @PostMapping("/register")
    public AjaxResult register(@RequestBody RegisterBody user)
    {
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return error("当前系统没有开启注册功能！");
        }
        String msg = registerService.register(user);
        return StringUtils.isEmpty(msg) ? success() : error(msg);
    }
}
