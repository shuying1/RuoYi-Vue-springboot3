package com.ruoyi.common.core.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * 树形结构实体基类
 * 
 * 继承 {@link BaseEntity}，增加树形结构所需的字段，
 * 包括父节点ID、父节点名称、排序号、祖先节点路径和子节点集合。
 * 适用于部门、菜单、区域等具有父子层级关系的业务实体。
 * 
 * @author ruoyi
 */
public class TreeEntity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /**
     * 父节点名称，用于前端树形组件展示父级标签
     */
    private String parentName;

    /**
     * 父节点ID，值为0表示顶级节点
     */
    private Long parentId;

    /**
     * 同层级节点的显示顺序，值越小越靠前
     */
    private Integer orderNum;

    /**
     * 祖级节点ID列表，格式如 "0,1,100"，用于快速判断层级深度和路径
     */
    private String ancestors;

    /**
     * 子节点集合，初始化为空ArrayList防止NPE
     */
    private List<?> children = new ArrayList<>();

    /**
     * 获取父节点名称
     * @return 父节点名称字符串
     */
    public String getParentName()
    {
        return parentName;
    }

    /**
     * 设置父节点名称
     * @param parentName 父节点名称
     */
    public void setParentName(String parentName)
    {
        this.parentName = parentName;
    }

    /**
     * 获取父节点ID
     * @return 父节点ID，0表示顶级
     */
    public Long getParentId()
    {
        return parentId;
    }

    /**
     * 设置父节点ID
     * @param parentId 父节点ID
     */
    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    /**
     * 获取排序号
     * @return 同层级节点的排序值
     */
    public Integer getOrderNum()
    {
        return orderNum;
    }

    /**
     * 设置排序号
     * @param orderNum 排序值，值越小越靠前
     */
    public void setOrderNum(Integer orderNum)
    {
        this.orderNum = orderNum;
    }

    /**
     * 获取祖先节点ID列表
     * @return 以逗号分隔的祖先ID字符串
     */
    public String getAncestors()
    {
        return ancestors;
    }

    /**
     * 设置祖先节点ID列表
     * @param ancestors 祖先ID字符串
     */
    public void setAncestors(String ancestors)
    {
        this.ancestors = ancestors;
    }

    /**
     * 获取子节点集合
     * @return 子节点列表，永不为null
     */
    public List<?> getChildren()
    {
        return children;
    }

    /**
     * 设置子节点集合
     * @param children 子节点列表
     */
    public void setChildren(List<?> children)
    {
        this.children = children;
    }
}
