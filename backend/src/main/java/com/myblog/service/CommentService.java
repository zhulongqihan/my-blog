package com.myblog.service;

import com.myblog.dto.CommentRequest;
import com.myblog.dto.CommentResponse;
import com.myblog.dto.mq.CommentNotificationMessage;
import com.myblog.entity.Article;
import com.myblog.entity.Comment;
import com.myblog.entity.User;
import com.myblog.repository.ArticleRepository;
import com.myblog.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;
    private final MQProducerService mqProducerService;

    public Page<CommentResponse> getCommentsByArticle(Long articleId, Pageable pageable) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        
        return commentRepository.findByArticleAndParentIsNullAndApprovedTrue(article, pageable)
                .map(this::toResponseWithReplies);
    }

    @Transactional
    public CommentResponse createComment(Long articleId, CommentRequest request, User user) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        Comment comment = Comment.builder()
                .content(request.getContent())
                .article(article)
                .user(user)
                .guestName(user == null ? request.getGuestName() : null)
                .guestEmail(user == null ? request.getGuestEmail() : null)
                .build();

        Comment parentComment = null;
        if (request.getParentId() != null) {
            parentComment = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));
            comment.setParent(parentComment);
        }

        Comment savedComment = commentRepository.save(comment);

        // 发送评论通知到MQ（异步邮件通知）
        try {
            sendCommentNotification(savedComment, article, parentComment, user);
        } catch (Exception e) {
            // MQ发送失败不影响评论创建
            log.error("发送评论通知消息失败", e);
        }

        return toResponse(savedComment);
    }

    /**
     * 构建并发送评论通知消息到MQ
     */
    private void sendCommentNotification(Comment comment, Article article, 
                                          Comment parentComment, User user) {
        String commenterName = user != null ? user.getNickname() : comment.getGuestName();
        String commenterEmail = user != null ? user.getEmail() : comment.getGuestEmail();

        CommentNotificationMessage.CommentNotificationMessageBuilder builder = 
                CommentNotificationMessage.builder()
                .commentId(comment.getId())
                .articleId(article.getId())
                .articleTitle(article.getTitle())
                .commentContent(comment.getContent())
                .commenterName(commenterName != null ? commenterName : "匿名用户")
                .commenterEmail(commenterEmail)
                .commentTime(LocalDateTime.now());

        // 如果是回复评论，获取被回复者信息
        if (parentComment != null) {
            builder.parentCommentId(parentComment.getId());
            if (parentComment.getUser() != null) {
                builder.parentCommenterEmail(parentComment.getUser().getEmail());
                builder.parentCommenterName(parentComment.getUser().getNickname());
            } else {
                builder.parentCommenterEmail(parentComment.getGuestEmail());
                builder.parentCommenterName(parentComment.getGuestName());
            }
        }

        mqProducerService.sendCommentNotification(builder.build());
    }

    @Transactional
    public void deleteComment(Long id, User currentUser) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("评论不存在"));

        if (comment.getUser() != null && !comment.getUser().getId().equals(currentUser.getId()) 
            && currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("无权删除此评论");
        }

        commentRepository.delete(comment);
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .user(comment.getUser() != null ? CommentResponse.UserInfo.builder()
                        .id(comment.getUser().getId())
                        .username(comment.getUser().getUsername())
                        .nickname(comment.getUser().getNickname())
                        .avatar(comment.getUser().getAvatar())
                        .build() : null)
                .guestName(comment.getGuestName())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private CommentResponse toResponseWithReplies(Comment comment) {
        CommentResponse response = toResponse(comment);
        response.setReplies(commentRepository.findByParentAndApprovedTrue(comment).stream()
                .map(this::toResponse)
                .collect(Collectors.toList()));
        return response;
    }
}
