package com.ruoyi.common.core.domain;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.domain.entity.SysDept;
import com.ruoyi.common.core.domain.entity.SysMenu;
import com.ruoyi.common.utils.StringUtils;

/**
 * 树形选择组件实体类
 * 
 * 用于前端下拉选择框、权限选择器等组件的数据传输。
 * 将部门（SysDept）和菜单（SysMenu）实体转换为通用的树形选择结构，
 * 仅保留节点ID、显示标签、禁用状态和子节点列表四个字段。
 *
 * 序列化时空的children集合会被自动排除（@JsonInclude NON_EMPTY）。
 * 
 * @author ruoyi
 */
public class TreeSelect implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * 节点唯一标识ID
     */
    private Long id;

    /**
     * 节点显示名称（标签）
     */
    private String label;

    /**
     * 节点是否禁用，默认为false。
     * 部门禁用时设置为true表示不允许选择该部门。
     */
    private boolean disabled = false;

    /**
     * 子节点列表，空集合序列化时自动排除
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<TreeSelect> children;

    /**
     * 默认构造函数
     */
    public TreeSelect()
    {

    }

    /**
     * 构造函数：从部门实体转换
     * 根据部门的status字段判断是否禁用，递归转换子部门为子节点
     * 
     * @param dept 部门实体对象
     */
    public TreeSelect(SysDept dept)
    {
        this.id = dept.getDeptId();
        this.label = dept.getDeptName();
        // 部门状态为停用（DEPT_DISABLE="1"）时标记为禁用
        this.disabled = StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus());
        // 递归转换子部门为TreeSelect子节点
        this.children = dept.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 构造函数：从菜单实体转换
     * 递归转换子菜单为子节点，菜单暂不支持禁用状态
     * 
     * @param menu 菜单实体对象
     */
    public TreeSelect(SysMenu menu)
    {
        this.id = menu.getMenuId();
        this.label = menu.getMenuName();
        this.children = menu.getChildren().stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 获取节点ID
     * @return 节点唯一标识
     */
    public Long getId()
    {
        return id;
    }

    /**
     * 设置节点ID
     * @param id 节点唯一标识
     */
    public void setId(Long id)
    {
        this.id = id;
    }

    /**
     * 获取节点显示标签
     * @return 显示名称
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * 设置节点显示标签
     * @param label 显示名称
     */
    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * 获取节点是否禁用状态
     * @return true=禁用不可选择，false=可选
     */
    public boolean isDisabled()
    {
        return disabled;
    }

    /**
     * 设置节点禁用状态
     * @param disabled true=禁用
     */
    public void setDisabled(boolean disabled)
    {
        this.disabled = disabled;
    }

    /**
     * 获取子节点列表
     * @return 子节点集合
     */
    public List<TreeSelect> getChildren()
    {
        return children;
    }

    /**
     * 设置子节点列表
     * @param children 子节点集合
     */
    public void setChildren(List<TreeSelect> children)
    {
        this.children = children;
    }
}
