package com.myblog.repository;

import com.myblog.dto.ArchiveArticleProjection;
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
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    @Query("SELECT new com.myblog.dto.ArticlePublicationVersion(a.id, a.updatedAt) " +
            "FROM Article a WHERE a.id = :id AND a.published = true")
    Optional<com.myblog.dto.ArticlePublicationVersion> findPublishedVersion(@Param("id") Long id);

    @Query("SELECT new com.myblog.dto.ArticlePublicationVersion(a.id, a.updatedAt) " +
            "FROM Article a WHERE a.published = true")
    List<com.myblog.dto.ArticlePublicationVersion> findAllPublishedVersions();

    boolean existsByIdAndPublishedTrue(Long id);

    Optional<Article> findByIdAndPublishedTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Article a WHERE a.id = :id")
    Optional<Article> findByIdForUpdate(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Article a SET a.viewCount = COALESCE(a.viewCount, 0) + :delta WHERE a.id = :id")
    int incrementViewCount(@Param("id") Long id, @Param("delta") int delta);
    
    Page<Article> findByPublishedTrue(Pageable pageable);
    
    Page<Article> findByPublishedTrueAndCategory(Category category, Pageable pageable);
    
    Page<Article> findByAuthor(User author, Pageable pageable);
    
    List<Article> findByFeaturedTrueAndPublishedTrue();
    
        @Query("SELECT DISTINCT a FROM Article a " +
            "LEFT JOIN a.category c " +
            "LEFT JOIN a.tags t " +
            "WHERE a.published = true AND (" +
            "LOWER(a.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(a.summary, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(t.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))")
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

    /**
     * 获取所有已发布文章（按创建时间倒序），用于归档页
     */
    List<Article> findByPublishedTrueOrderByPublishedAtDesc();

    @Query("SELECT a.id AS id, a.title AS title, a.publishedAt AS publishedAt, " +
            "a.createdAt AS createdAt, c.name AS categoryName " +
            "FROM Article a LEFT JOIN a.category c " +
            "WHERE a.published = true ORDER BY a.publishedAt DESC")
    List<ArchiveArticleProjection> findPublishedArchiveRows();

    // ========== v2.0 新增：Redis 高并发特性 ==========

    /**
     * 查询所有已发布文章ID（布隆过滤器初始化用）
     */
    @Query("SELECT a.id FROM Article a WHERE a.published = true")
    List<Long> findAllPublishedArticleIds();
}
