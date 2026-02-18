package com.myblog.repository;

import com.myblog.entity.SysMenu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 菜单Repository
 */
@Repository
public interface SysMenuRepository extends JpaRepository<SysMenu, Long> {

    /**
     * 查询所有有效菜单（按排序顺序）
     */
    List<SysMenu> findByIsDeletedOrderBySortOrder(Integer isDeleted);

    /**
     * 根据父菜单ID查询子菜单
     */
    List<SysMenu> findByParentIdAndIsDeletedOrderBySortOrder(Long parentId, Integer isDeleted);

    /**
     * 根据菜单类型查询
     */
    List<SysMenu> findByMenuTypeAndIsDeletedOrderBySortOrder(String menuType, Integer isDeleted);

    /**
     * 查询所有可见菜单
     */
    List<SysMenu> findByVisibleAndIsDeletedOrderBySortOrder(Integer visible, Integer isDeleted);

    /**
     * 根据角色ID查询菜单列表
     */
    @Query("SELECT m FROM SysMenu m JOIN m.roles r WHERE r.id = :roleId AND m.isDeleted = 0 ORDER BY m.sortOrder")
    List<SysMenu> findByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据用户ID查询菜单列表（通过角色关联）
     */
    @Query("SELECT DISTINCT m FROM SysMenu m JOIN m.roles r JOIN r.users u WHERE u.id = :userId AND m.isDeleted = 0 ORDER BY m.sortOrder")
    List<SysMenu> findByUserId(@Param("userId") Long userId);

    /**
     * 根据权限标识查询
     */
    List<SysMenu> findByPermissionAndIsDeleted(String permission, Integer isDeleted);

    /**
     * 查询顶级菜单（parentId = 0）
     */
    List<SysMenu> findByParentIdAndVisibleAndIsDeletedOrderBySortOrder(Long parentId, Integer visible, Integer isDeleted);
}
