package com.ruoyi.system.domain.vo;

import com.ruoyi.common.utils.StringUtils;

/**
 * 路由显示信息（Meta 视图对象）
 *
 * 封装前端 Vue Router 中每个路由的 meta 配置信息。
 * 登录成功后，SysMenuServiceImpl.buildMenus() 方法会将数据库中的菜单记录（SysMenu）
 * 转换为 RouterVo 列表，其中每个 RouterVo 的 meta 字段即为本类型对象。
 * 前端根据 title 渲染侧边栏和面包屑的文字，icon 渲染图标，noCache 控制 keep-alive 缓存，
 * link 用于内嵌外链场景（如 iframe 嵌入外部系统）。
 *
 * @author ruoyi
 */
public class MetaVo
{
    /**
     * 设置该路由在侧边栏和面包屑中展示的名字
     */
    private String title;

    /**
     * 设置该路由的图标，对应路径src/assets/icons/svg
     */
    private String icon;

    /**
     * 设置为true，则不会被 <keep-alive>缓存
     */
    private boolean noCache;

    /**
     * 内链地址（http(s)://开头）
     */
    private String link;

    public MetaVo()
    {
    }

    public MetaVo(String title, String icon)
    {
        this.title = title;
        this.icon = icon;
    }

    public MetaVo(String title, String icon, boolean noCache)
    {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
    }

    public MetaVo(String title, String icon, String link)
    {
        this.title = title;
        this.icon = icon;
        this.link = link;
    }

    public MetaVo(String title, String icon, boolean noCache, String link)
    {
        this.title = title;
        this.icon = icon;
        this.noCache = noCache;
        if (StringUtils.ishttp(link))
        {
            this.link = link;
        }
    }

    public boolean isNoCache()
    {
        return noCache;
    }

    public void setNoCache(boolean noCache)
    {
        this.noCache = noCache;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getIcon()
    {
        return icon;
    }

    public void setIcon(String icon)
    {
        this.icon = icon;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }
}
