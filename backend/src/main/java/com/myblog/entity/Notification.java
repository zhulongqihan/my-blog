package com.myblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知实体
 * 
 * 存储所有类型的站内通知，支持：
 * - COMMENT：新评论通知（通知博主/被回复者）
 * - SYSTEM：系统公告（管理员发布，全站广播）
 * - LIKE：点赞通知（预留）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_recipient", columnList = "recipientId"),
    @Index(name = "idx_notification_read", columnList = "recipientId, isRead"),
    @Index(name = "idx_notification_created", columnList = "createdAt")
})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 通知类型 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    /** 通知标题 */
    @Column(nullable = false)
    private String title;

    /** 通知内容 */
    @Column(columnDefinition = "TEXT")
    private String content;

    /** 接收者用户ID（null 表示全站广播） */
    private Long recipientId;

    /** 发送者用户名 */
    private String senderName;

    /** 关联资源ID（如文章ID、评论ID） */
    private Long relatedId;

    /** 关联资源类型（article / comment） */
    private String relatedType;

    /** 是否已读 */
    @Builder.Default
    private Boolean isRead = false;

    /** 创建时间 */
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        COMMENT,   // 评论通知
        SYSTEM,    // 系统公告
        LIKE       // 点赞通知（预留）
    }
}
