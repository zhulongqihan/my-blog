package com.myblog.repository;

import com.myblog.entity.Article;
import com.myblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByArticleAndParentIsNullAndApprovedTrue(Article article, Pageable pageable);
    List<Comment> findByParentAndApprovedTrue(Comment parent);
    long countByArticle(Article article);

    // ========== 管理后台统计查询 ==========

    /**
     * 统计指定时间范围内创建的评论数
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 获取最新评论（按创建时间倒序）
     */
    @Query("SELECT c FROM Comment c ORDER BY c.createdAt DESC")
    List<Comment> findTopByOrderByCreatedAtDesc(@Param("limit") int limit);

    /**
     * 分页获取所有评论（管理后台用）
     */
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 根据审核状态获取评论
     */
    Page<Comment> findByApprovedOrderByCreatedAtDesc(boolean approved, Pageable pageable);
}
