package com.myblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 系统菜单/权限实体
 * 用于RBAC权限控制，支持树形结构
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_menu")
public class SysMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 父菜单ID，顶级菜单为0
     */
    @Column(name = "parent_id")
    @Builder.Default
    private Long parentId = 0L;

    /**
     * 菜单名称
     */
    @Column(name = "menu_name", nullable = false, length = 50)
    private String menuName;

    /**
     * 菜单类型：D-目录，M-菜单，B-按钮
     */
    @Column(name = "menu_type", length = 1)
    @Builder.Default
    private String menuType = "M";

    /**
     * 路由地址
     */
    @Column(length = 200)
    private String path;

    /**
     * 组件路径
     */
    @Column(length = 200)
    private String component;

    /**
     * 权限标识
     * 如：system:user:list
     */
    @Column(name = "permission", length = 100)
    private String permission;

    /**
     * 菜单图标
     */
    @Column(length = 100)
    private String icon;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    /**
     * 是否可见：0-隐藏，1-显示
     */
    @Builder.Default
    private Integer visible = 1;

    /**
     * 状态：0-禁用，1-启用
     */
    @Builder.Default
    private Integer status = 1;

    /**
     * 是否删除：0-否，1-是
     */
    @Column(name = "is_deleted")
    @Builder.Default
    private Integer isDeleted = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 拥有该菜单权限的角色
     */
    @ManyToMany(mappedBy = "menus", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<SysRole> roles = new HashSet<>();

    /**
     * 子菜单（非持久化，用于树形结构展示）
     */
    @Transient
    @Builder.Default
    private Set<SysMenu> children = new HashSet<>();
}
