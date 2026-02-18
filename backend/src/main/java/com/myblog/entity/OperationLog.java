package com.myblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 操作日志实体
 * 设计目标：记录用户的所有操作，用于审计和分析
 * 
 * 技术说明：使用JPA注解（移除了MyBatis-Plus依赖，避免与Spring Boot 3.x冲突）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "operation_log")
public class OperationLog {
    
    /**
     * 日志ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 操作用户ID
     */
    private Long userId;
    
    /**
     * 操作用户名
     */
    private String username;
    
    /**
     * 操作类型：CREATE/UPDATE/DELETE/QUERY
     */
    private String operationType;
    
    /**
     * 操作模块：ARTICLE/COMMENT/USER等
     */
    private String module;
    
    /**
     * 操作描述
     */
    private String description;
    
    /**
     * 请求方法（类名.方法名）
     */
    private String method;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * HTTP方法：GET/POST/PUT/DELETE
     */
    private String requestMethod;
    
    /**
     * 请求参数（JSON）
     */
    @Column(columnDefinition = "TEXT")
    private String requestParams;
    
    /**
     * 响应结果（JSON，可选）
     */
    @Column(columnDefinition = "TEXT")
    private String responseResult;
    
    /**
     * 操作IP
     */
    private String ipAddress;
    
    /**
     * IP归属地
     */
    private String location;
    
    /**
     * 浏览器
     */
    private String browser;
    
    /**
     * 操作系统
     */
    private String os;
    
    /**
     * 执行耗时（毫秒）
     */
    private Integer executionTime;
    
    /**
     * 状态：0-失败 1-成功
     */
    private Integer status;
    
    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMsg;
    
    /**
     * 操作时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
