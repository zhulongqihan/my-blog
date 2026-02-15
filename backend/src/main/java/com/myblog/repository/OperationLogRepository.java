package com.myblog.repository;

import com.myblog.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志Repository
 * 使用JPA替代MyBatis-Plus，避免与Spring Data JPA冲突
 * 
 * 设计说明：
 * - 继承JpaRepository自动获得CRUD方法
 * - 支持方法名查询（Spring Data JPA特性）
 * - 避免MyBatis-Plus和JPA的factoryBean冲突
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    
    /**
     * 根据用户ID查询操作日志
     * Spring Data JPA会自动实现此方法
     */
    List<OperationLog> findByUserId(Long userId);
    
    /**
     * 根据模块查询操作日志
     */
    List<OperationLog> findByModule(String module);
    
    /**
     * 根据时间范围查询操作日志
     */
    List<OperationLog> findByCreatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 根据用户ID和时间范围查询
     */
    List<OperationLog> findByUserIdAndCreatedAtBetween(Long userId, LocalDateTime startTime, LocalDateTime endTime);
}
