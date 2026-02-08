package com.myblog.repository;

import com.myblog.entity.Article;
import com.myblog.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    Page<Comment> findByArticleAndParentIsNullAndApprovedTrue(Article article, Pageable pageable);
    List<Comment> findByParentAndApprovedTrue(Comment parent);
    long countByArticle(Article article);
}
