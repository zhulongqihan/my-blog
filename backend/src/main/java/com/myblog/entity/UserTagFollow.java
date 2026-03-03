package com.myblog.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户-标签关注关系实体
 *
 * 支撑 Feed 流推模型：用户关注标签 → 该标签下新文章推入用户 Feed
 */
@Entity
@Table(name = "user_tag_follow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "tag_id"}),
        indexes = {
                @Index(name = "idx_follow_user", columnList = "user_id"),
                @Index(name = "idx_follow_tag", columnList = "tag_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserTagFollow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 关注者用户 ID */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 被关注的标签 ID */
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    /** 关注时间 */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
