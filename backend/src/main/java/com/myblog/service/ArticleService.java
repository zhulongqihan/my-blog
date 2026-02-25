package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.dto.ArticleRequest;
import com.myblog.dto.ArticleResponse;
import com.myblog.entity.*;
import com.myblog.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CommentRepository commentRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Page<ArticleResponse> getPublishedArticles(Pageable pageable) {
        return articleRepository.findByPublishedTrue(pageable)
                .map(this::toResponse);
    }

    public Page<ArticleResponse> getArticlesByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        return articleRepository.findByPublishedTrueAndCategory(category, pageable)
                .map(this::toResponse);
    }

    public Page<ArticleResponse> getArticlesByTag(Long tagId, Pageable pageable) {
        return articleRepository.findByTagId(tagId, pageable)
                .map(this::toResponse);
    }

    public Page<ArticleResponse> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable)
                .map(this::toResponse);
    }

    /**
     * 获取精选文章（缓存10分钟）
     * 使用 @Cacheable：第一次查询走DB，后续读缓存
     */
    @Cacheable(value = "featuredArticles", key = "'all'")
    public List<ArticleResponse> getFeaturedArticles() {
        log.info("[Cache MISS] 精选文章 - 从数据库加载");
        return articleRepository.findByFeaturedTrueAndPublishedTrue().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门文章（缓存10分钟）
     */
    @Cacheable(value = "popularArticles", key = "#limit")
    public List<ArticleResponse> getPopularArticles(int limit) {
        log.info("[Cache MISS] 热门文章(limit={}) - 从数据库加载", limit);
        return articleRepository.findTopByViewCount(Pageable.ofSize(limit)).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取文章详情（缓存30分钟）
     * Cache Aside 模式：先查缓存，未命中再查DB并回填缓存
     */
    @Cacheable(value = "articleDetail", key = "#id")
    public ArticleResponse getArticle(Long id) {
        log.info("[Cache MISS] 文章详情(id={}) - 从数据库加载", id);
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));
        return toResponse(article);
    }

    /**
     * 获取文章详情并增加浏览量
     * 改造前：每次访问直接写DB（性能差）
     * 改造后：浏览量用Redis INCR累加（高性能），定时任务同步回DB
     * 
     * 同时记录每日访问量，解决 DashboardService 的 todayViews TODO
     */
    public ArticleResponse getArticleAndIncrementView(Long id) {
        // 1. 浏览量在Redis中累加（原子操作，不写DB）
        String viewKey = RedisKeyPrefix.ARTICLE_VIEW_COUNT + id;
        redisTemplate.opsForValue().increment(viewKey);
        
        // 2. 记录今日访问量（用于仪表盘统计）
        String todayKey = RedisKeyPrefix.DAILY_VIEW_COUNT 
                + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForValue().increment(todayKey);
        
        // 3. 从缓存获取文章详情（复用 @Cacheable 方法）
        return getArticle(id);
    }

    /**
     * 创建文章 → 清除列表缓存（精选/热门可能变化）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "featuredArticles", allEntries = true),
        @CacheEvict(value = "popularArticles", allEntries = true)
    })
    public ArticleResponse createArticle(ArticleRequest request, User author) {
        Article article = Article.builder()
                .title(request.getTitle())
                .summary(request.getSummary())
                .content(request.getContent())
                .coverImage(request.getCoverImage())
                .author(author)
                .published(request.getPublished())
                .featured(request.getFeatured())
                .build();

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("分类不存在"));
            article.setCategory(category);
        }

        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            article.setTags(tags);
        }

        if (Boolean.TRUE.equals(request.getPublished())) {
            article.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(articleRepository.save(article));
    }

    /**
     * 更新文章 → 清除该文章缓存 + 列表缓存
     * @CacheEvict 保证缓存一致性（Cache Aside 写策略：先更新DB，再删缓存）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "articleDetail", key = "#id"),
        @CacheEvict(value = "featuredArticles", allEntries = true),
        @CacheEvict(value = "popularArticles", allEntries = true)
    })
    public ArticleResponse updateArticle(Long id, ArticleRequest request, User currentUser) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) 
            && currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("无权修改此文章");
        }

        article.setTitle(request.getTitle());
        article.setSummary(request.getSummary());
        article.setContent(request.getContent());
        article.setCoverImage(request.getCoverImage());
        article.setPublished(request.getPublished());
        article.setFeatured(request.getFeatured());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("分类不存在"));
            article.setCategory(category);
        }

        if (request.getTagIds() != null) {
            List<Tag> tags = tagRepository.findAllById(request.getTagIds());
            article.setTags(tags);
        }

        if (Boolean.TRUE.equals(request.getPublished()) && article.getPublishedAt() == null) {
            article.setPublishedAt(LocalDateTime.now());
        }

        return toResponse(articleRepository.save(article));
    }

    /**
     * 删除文章 → 清除所有相关缓存
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "articleDetail", key = "#id"),
        @CacheEvict(value = "featuredArticles", allEntries = true),
        @CacheEvict(value = "popularArticles", allEntries = true)
    })
    public void deleteArticle(Long id, User currentUser) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) 
            && currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("无权删除此文章");
        }

        articleRepository.delete(article);
    }

    private ArticleResponse toResponse(Article article) {
        return ArticleResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
                .content(article.getContent())
                .coverImage(article.getCoverImage())
                .author(article.getAuthor() != null ? ArticleResponse.AuthorInfo.builder()
                        .id(article.getAuthor().getId())
                        .username(article.getAuthor().getUsername())
                        .nickname(article.getAuthor().getNickname())
                        .avatar(article.getAuthor().getAvatar())
                        .build() : null)
                .category(article.getCategory() != null ? ArticleResponse.CategoryInfo.builder()
                        .id(article.getCategory().getId())
                        .name(article.getCategory().getName())
                        .icon(article.getCategory().getIcon())
                        .build() : null)
                .tags(article.getTags().stream()
                        .map(tag -> ArticleResponse.TagInfo.builder()
                                .id(tag.getId())
                                .name(tag.getName())
                                .color(tag.getColor())
                                .build())
                        .collect(Collectors.toList()))
                .viewCount(article.getViewCount())
                .likeCount(article.getLikeCount())
                .commentCount(commentRepository.countByArticle(article))
                .published(article.getPublished())
                .featured(article.getFeatured())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }
}
