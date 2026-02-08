package com.myblog.service;

import com.myblog.dto.CommentRequest;
import com.myblog.dto.CommentResponse;
import com.myblog.entity.Article;
import com.myblog.entity.Comment;
import com.myblog.entity.User;
import com.myblog.repository.ArticleRepository;
import com.myblog.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final ArticleRepository articleRepository;

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

        if (request.getParentId() != null) {
            Comment parent = commentRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("父评论不存在"));
            comment.setParent(parent);
        }

        return toResponse(commentRepository.save(comment));
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
