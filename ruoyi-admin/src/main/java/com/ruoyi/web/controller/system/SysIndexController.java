package com.ruoyi.web.controller.system;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.config.RuoYiConfig;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.service.ISysUserService;

/**
 * 首页与屏幕解锁接口控制器
 * 
 * <p>提供默认欢迎页提示语（部署在根路径）以及"锁屏"场景下的解锁验证接口。</p>
 * 
 * <p>API 列表：</p>
 * <ul>
 *   <li>{@code GET  /}             — 返回欢迎语字符串（浏览器直接访问后端地址时提示通过前端访问）</li>
 *   <li>{@code POST /unlockscreen} — 锁屏解锁验证（校验当前用户密码）</li>
 * </ul>
 * 
 * @author ruoyi
 */
@RestController
public class SysIndexController
{
    /** 系统基础配置，读取项目名称与版本号 */
    @Autowired
    private RuoYiConfig ruoyiConfig;

    @Autowired
    private ISysUserService userService;

    /**
     * 访问后端根路径的欢迎提示
     * 
     * <p>GET / 或 ANY /</p>
     * 
     * @return 欢迎字符串，包含系统名与版本号
     */
    @RequestMapping("/")
    public String index()
    {
        return StringUtils.format("欢迎使用{}后台管理框架，当前版本：v{}，请通过前端地址访问。", ruoyiConfig.getName(), ruoyiConfig.getVersion());
    }

    /**
     * 锁屏解锁验证
     * 
     * <p>POST /unlockscreen</p>
     * 
     * <p>请求体：{@code {"password": "当前登录密码"}}</p>
     * <p>校验当前用户密码是否正确；密码错误或用户已不存在（被停用/删除）时返回错误。</p>
     * 
     * @param body 包含 password 的请求体
     * @return AjaxResult
     */
    @PostMapping("/unlockscreen")
    public AjaxResult unlockScreen(@RequestBody Map<String, String> body)
    {
        String password = body.get("password");
        if (StringUtils.isEmpty(password))
        {
            return AjaxResult.error("密码不能为空");
        }
        String username = SecurityUtils.getUsername();
        SysUser user = userService.selectUserByUserName(username);
        if (user == null)
        {
            return AjaxResult.error("服务器超时，请重新登录");
        }
        if (!SecurityUtils.matchesPassword(password, user.getPassword()))
        {
            return AjaxResult.error("密码错误，请重新输入");
        }

        return AjaxResult.success("解锁成功");
    }
}
