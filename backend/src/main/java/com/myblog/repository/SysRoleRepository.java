package com.myblog.repository;

import com.myblog.entity.SysRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色Repository
 */
@Repository
public interface SysRoleRepository extends JpaRepository<SysRole, Long> {

    /**
     * 根据角色编码查询
     */
    Optional<SysRole> findByRoleCode(String roleCode);

    /**
     * 根据角色编码查询（排除已删除）
     */
    Optional<SysRole> findByRoleCodeAndIsDeleted(String roleCode, Integer isDeleted);

    /**
     * 查询所有有效角色
     */
    List<SysRole> findByIsDeletedOrderBySortOrder(Integer isDeleted);

    /**
     * 查询所有启用的角色
     */
    List<SysRole> findByStatusAndIsDeletedOrderBySortOrder(Integer status, Integer isDeleted);

    /**
     * 根据用户ID查询角色列表
     */
    @Query("SELECT r FROM SysRole r JOIN r.users u WHERE u.id = :userId AND r.isDeleted = 0")
    List<SysRole> findByUserId(@Param("userId") Long userId);

    /**
     * 检查角色编码是否存在
     */
    boolean existsByRoleCodeAndIsDeleted(String roleCode, Integer isDeleted);
}
