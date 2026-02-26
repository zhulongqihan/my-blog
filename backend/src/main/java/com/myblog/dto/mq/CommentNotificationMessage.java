package com.myblog.dto.mq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 评论通知消息 DTO
 * 
 * 当用户发表评论时，通过 MQ 异步发送邮件通知：
 * - 新评论 → 通知博主
 * - 回复评论 → 通知被回复者
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentNotificationMessage implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /** 消息ID（幂等去重） */
    private String messageId;

    /** 评论ID */
    private Long commentId;

    /** 文章ID */
    private Long articleId;

    /** 文章标题 */
    private String articleTitle;

    /** 评论内容 */
    private String commentContent;

    /** 评论人名称 */
    private String commenterName;

    /** 评论人邮箱（用于回复通知） */
    private String commenterEmail;

    /** 父评论ID（如果是回复） */
    private Long parentCommentId;

    /** 被回复者邮箱 */
    private String parentCommenterEmail;

    /** 被回复者名称 */
    private String parentCommenterName;

    /** 评论时间 */
    private LocalDateTime commentTime;
}
