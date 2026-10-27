package com.ruoyi.web.controller.monitor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.CacheConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SysUserOnline;
import com.ruoyi.system.service.ISysUserOnlineService;

/**
 * 在线用户监控 Controller
 *
 * <p>业务模块：系统监控 — 在线用户管理，路由前缀 {@code /monitor/online}。
 * 在线用户列表不是从数据库查询，而是实时扫描 Redis 中 {@code login_tokens:*} 的所有登录会话，
 * 再结合 {@link ISysUserOnlineService} 做 IP/用户名筛选。强退用户即删除对应 token。</p>
 *
 * <p>依赖 Service：{@link ISysUserOnlineService}（筛选与转换工具方法）、
 * {@link RedisCache}（登录会话存储）。</p>
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/monitor/online")
public class SysUserOnlineController extends BaseController
{
    /** 在线用户查询 Service */
    @Autowired
    private ISysUserOnlineService userOnlineService;

    /** Redis 客户端：登录态以 {@code login_tokens:{uuid}} 形式缓存 */
    @Autowired
    private RedisCache redisCache;

    /**
     * 分页查询在线用户列表
     *
     * <p>url: GET /monitor/online/list</p>
     *
     * <p>实时从 Redis 扫描所有 token，支持按登录 IP 与用户名过滤；
     * 结果按登录时间倒序（新会话在前）。</p>
     *
     * @param url 请求路径
     * @param ipaddr 可选，登录 IP 精确匹配条件
     * @param userName 可选，登录账号精确匹配条件
     * @return TableDataInfo 在线用户列表
     */
    @PreAuthorize("@ss.hasPermi('monitor:online:list')")
    @GetMapping("/list")
    public TableDataInfo list(String ipaddr, String userName)
    {
        Collection<String> keys = redisCache.keys(CacheConstants.LOGIN_TOKEN_KEY + "*");
        List<SysUserOnline> userOnlineList = new ArrayList<SysUserOnline>();
        for (String key : keys)
        {
            LoginUser user = redisCache.getCacheObject(key);
            if (StringUtils.isNotEmpty(ipaddr) && StringUtils.isNotEmpty(userName))
            {
                userOnlineList.add(userOnlineService.selectOnlineByInfo(ipaddr, userName, user));
            }
            else if (StringUtils.isNotEmpty(ipaddr))
            {
                userOnlineList.add(userOnlineService.selectOnlineByIpaddr(ipaddr, user));
            }
            else if (StringUtils.isNotEmpty(userName) && StringUtils.isNotNull(user.getUser()))
            {
                userOnlineList.add(userOnlineService.selectOnlineByUserName(userName, user));
            }
            else
            {
                userOnlineList.add(userOnlineService.loginUserToUserOnline(user));
            }
        }
        Collections.reverse(userOnlineList);
        userOnlineList.removeAll(Collections.singleton(null));
        return getDataTable(userOnlineList);
    }

    /**
     * 强退用户（删除登录 token）
     *
     * <p>url: DELETE /monitor/online/{tokenId}</p>
     *
     * <p>底层直接删除 Redis 中 {@code login_tokens:{tokenId}}，
     * 被强退用户下一次请求 JWT 校验时会被拦截，需重新登录。</p>
     *
     * @param url 请求路径
     * @param tokenId 需要强退的会话 token uuid（Redis key 的后缀）
     * @return AjaxResult#success()
     */
    @PreAuthorize("@ss.hasPermi('monitor:online:forceLogout')")
    @Log(title = "在线用户", businessType = BusinessType.FORCE)
    @DeleteMapping("/{tokenId}")
    public AjaxResult forceLogout(@PathVariable String tokenId)
    {
        redisCache.deleteObject(CacheConstants.LOGIN_TOKEN_KEY + tokenId);
        return success();
    }
}
