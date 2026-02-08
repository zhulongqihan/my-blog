package com.myblog.repository;

import com.myblog.entity.Article;
import com.myblog.entity.Category;
import com.myblog.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    Page<Article> findByPublishedTrue(Pageable pageable);
    
    Page<Article> findByPublishedTrueAndCategory(Category category, Pageable pageable);
    
    Page<Article> findByAuthor(User author, Pageable pageable);
    
    List<Article> findByFeaturedTrueAndPublishedTrue();
    
    @Query("SELECT a FROM Article a WHERE a.published = true AND " +
           "(LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Article> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    @Query("SELECT a FROM Article a WHERE a.published = true ORDER BY a.viewCount DESC")
    List<Article> findTopByViewCount(Pageable pageable);
    
    @Query("SELECT a FROM Article a JOIN a.tags t WHERE t.id = :tagId AND a.published = true")
    Page<Article> findByTagId(@Param("tagId") Long tagId, Pageable pageable);
}
