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

import java.time.LocalDateTime;
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

    // ========== 管理后台统计查询 ==========

    /**
     * 统计指定时间范围内创建的文章数
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 获取总访问量
     */
    @Query("SELECT COALESCE(SUM(a.viewCount), 0) FROM Article a")
    Long sumViewCount();

    /**
     * 获取热门文章（按访问量排序）
     */
    @Query("SELECT a FROM Article a ORDER BY a.viewCount DESC")
    List<Article> findTopByViewCount(@Param("limit") int limit);

    /**
     * 统计指定分类下的文章数
     */
    @Query("SELECT COUNT(a) FROM Article a WHERE a.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
