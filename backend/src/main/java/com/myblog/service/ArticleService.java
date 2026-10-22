package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.exception.BusinessException;
import com.myblog.common.redis.CacheClient;
import com.myblog.dto.ArchiveArticleProjection;
import com.myblog.dto.ArchiveResponse;
import com.myblog.dto.ArticleRequest;
import com.myblog.dto.ArticleResponse;
import com.myblog.dto.ArticleSummaryResponse;
import com.myblog.dto.ArticlePublicationVersion;
import com.myblog.dto.LikeResponseDTO;
import com.myblog.entity.*;
import com.myblog.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
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
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    private final RedissonClient redissonClient;
    private final ApplicationEventPublisher publicationEvents;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private ArticleService self;

    /** FeedService 延迟注入，避免循环依赖 */
    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.context.annotation.Lazy
    private FeedService feedService;

    // ========== Lua 脚本：点赞 Toggle ==========
    private static final DefaultRedisScript<List<Long>> LIKE_TOGGLE_SCRIPT;
    static {
        LIKE_TOGGLE_SCRIPT = new DefaultRedisScript<>();
        LIKE_TOGGLE_SCRIPT.setLocation(new ClassPathResource("scripts/like_toggle.lua"));
        @SuppressWarnings("unchecked")
        Class<List<Long>> resultType = (Class<List<Long>>) (Class<?>) List.class;
        LIKE_TOGGLE_SCRIPT.setResultType(resultType);
    }

    public Page<ArticleSummaryResponse> getPublishedArticles(Pageable pageable) {
        return articleRepository.findByPublishedTrue(pageable)
                .map(this::toSummaryResponse);
    }

    public Page<ArticleSummaryResponse> getArticlesByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new RuntimeException("分类不存在"));
        return articleRepository.findByPublishedTrueAndCategory(category, pageable)
                .map(this::toSummaryResponse);
    }

    public Page<ArticleSummaryResponse> getArticlesByTag(Long tagId, Pageable pageable) {
        return articleRepository.findByTagId(tagId, pageable)
                .map(this::toSummaryResponse);
    }

    public Page<ArticleSummaryResponse> searchArticles(String keyword, Pageable pageable) {
        return articleRepository.searchByKeyword(keyword, pageable)
                .map(this::toSummaryResponse);
    }

    /**
     * 获取精选文章（缓存10分钟）
     * 使用 @Cacheable：第一次查询走DB，后续读缓存
     */
    @Transactional(readOnly = true)
    public List<ArticleSummaryResponse> getFeaturedArticles() {
        return onlyPublished(self.getFeaturedArticlesCached()).stream()
                .filter(summary -> Boolean.TRUE.equals(summary.getFeatured())).toList();
    }

    @Cacheable(value = "featuredArticleSummaries", key = "'all'")
    public List<ArticleSummaryResponse> getFeaturedArticlesCached() {
        log.info("[Cache MISS] 精选文章 - 从数据库加载");
        return articleRepository.findByFeaturedTrueAndPublishedTrue().stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取热门文章（缓存10分钟）
     */
    @Transactional(readOnly = true)
    public List<ArticleSummaryResponse> getPopularArticles(int limit) {
        return onlyPublished(self.getPopularArticlesCached(limit));
    }

    @Cacheable(value = "popularArticleSummaries", key = "#limit")
    public List<ArticleSummaryResponse> getPopularArticlesCached(int limit) {
        log.info("[Cache MISS] 热门文章(limit={}) - 从数据库加载", limit);
        return articleRepository.findTopByViewCount(Pageable.ofSize(limit)).stream()
                .map(this::toSummaryResponse)
                .collect(Collectors.toList());
    }

    /**
     * 获取文章详情 — 缓存三重防御
     *
     * 改造：移除 @Cacheable，改用 CacheClient 手动查询
     * ① 精选文章 → 逻辑过期方案（高可用，零延迟）
     * ② 普通文章 → 布隆过滤器 + 互斥锁方案（强一致性）
     */
    public ArticleResponse getArticle(Long id) {
        ArticlePublicationVersion version = articleRepository.findPublishedVersion(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
        // 先尝试逻辑过期方案（精选/热门文章已预热）
        ArticleResponse result = cacheClient.queryWithLogicalExpire(
                RedisKeyPrefix.ARTICLE_DETAIL_LOGIC, id, ArticleResponse.class,
                this::getArticleFromDb, 30L, TimeUnit.MINUTES
        );
        if (result != null) return currentVersionOrDatabase(id, version, result);

        // 普通文章在数据库可见性检查之后使用互斥锁缓存。
        // Publication was checked against the database; a stale Bloom filter must not hide a newly published article.
        result = cacheClient.queryWithMutex(
                RedisKeyPrefix.ARTICLE_DETAIL, id, ArticleResponse.class,
                this::getArticleFromDb, 30L, TimeUnit.MINUTES
        );

        if (result == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        }
        return currentVersionOrDatabase(id, version, result);
    }

    private ArticleResponse currentVersionOrDatabase(Long id, ArticlePublicationVersion version, ArticleResponse cached) {
        if (Boolean.TRUE.equals(cached.getPublished()) && version.updatedAt() != null
                && version.updatedAt().equals(cached.getUpdatedAt())) return cached;
        // A late rebuild or a failed invalidation must never revive the previous publication.
        try { cacheClient.evictArticle(id); }
        catch (org.springframework.dao.DataAccessException exception) { log.warn("Cannot evict stale article cache, using database"); }
        ArticleResponse current = self.loadPublishedArticleFromDb(id);
        if (current == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在");
        return current;
    }

    /** 从 DB 加载文章详情（供 CacheClient 回调使用） */
    private ArticleResponse getArticleFromDb(Long id) {
        return self.loadPublishedArticleFromDb(id);
    }

    @Transactional(readOnly = true)
    public ArticleResponse loadPublishedArticleFromDb(Long id) {
        return articleRepository.findByIdAndPublishedTrue(id)
                .map(this::toResponse)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public ArticleResponse getAdminArticle(Long id) {
        return articleRepository.findById(id).map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
    }

    private List<ArticleSummaryResponse> onlyPublished(List<ArticleSummaryResponse> summaries) {
        Map<Long, java.time.LocalDateTime> versions = articleRepository.findAllPublishedVersions().stream()
                .collect(HashMap::new, (map, version) -> map.put(version.id(), version.updatedAt()), HashMap::putAll);
        return summaries.stream().filter(summary -> versions.containsKey(summary.getId()))
                .map(summary -> versions.get(summary.getId()) != null && Objects.equals(versions.get(summary.getId()), summary.getUpdatedAt()) ? summary
                        : articleRepository.findByIdAndPublishedTrue(summary.getId()).map(this::toSummaryResponse).orElse(null))
                .filter(Objects::nonNull).toList();
    }

    /**
     * 获取文章详情 + 递增浏览量 + 记录 UV
     *
     * PV：Redis INCR（原子计数器）
     * UV：HyperLogLog PFADD（概率去重，12KB/文章）
     */
    public ArticleResponse getArticleAndIncrementView(Long id, HttpServletRequest request) {
        ArticleResponse response = getArticle(id);
        if ("HEAD".equalsIgnoreCase(request.getMethod())) return response;
        // ① PV: 浏览量在Redis中累加
        String viewKey = RedisKeyPrefix.ARTICLE_VIEW_COUNT + id;
        redisTemplate.opsForValue().increment(viewKey);

        String todayKey = RedisKeyPrefix.DAILY_VIEW_COUNT
                + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForValue().increment(todayKey);

        // ② UV: HyperLogLog PFADD（文章级）
        String fingerprint = generateFingerprint(request, getCurrentUser());
        stringRedisTemplate.opsForHyperLogLog().add(
                RedisKeyPrefix.ARTICLE_UV + id, fingerprint
        );

        // ③ UV: HyperLogLog PFADD（全站日级）
        stringRedisTemplate.opsForHyperLogLog().add(
                RedisKeyPrefix.STATS_UV_DAILY + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                fingerprint
        );

        // ④ 获取文章详情

        // ⑤ 附加 UV 数
        Long uvCount = stringRedisTemplate.opsForHyperLogLog().size(
                RedisKeyPrefix.ARTICLE_UV + id
        );
        response.setUvCount(uvCount != null ? uvCount : 0L);

        return response;
    }

    /** 向下兼容：无 request 参数的 getArticleAndIncrementView */
    public ArticleResponse getArticleAndIncrementView(Long id) {
        ArticleResponse response = getArticle(id);
        String viewKey = RedisKeyPrefix.ARTICLE_VIEW_COUNT + id;
        redisTemplate.opsForValue().increment(viewKey);
        String todayKey = RedisKeyPrefix.DAILY_VIEW_COUNT
                + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForValue().increment(todayKey);
        return response;
    }

    /**
     * 创建文章 → 清除列表缓存（精选/热门可能变化）
     */
    @Transactional
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

        Article savedArticle = articleRepository.save(article);

        publicationEvents.publishEvent(new ArticlePublicationChanged(savedArticle, false, false));

        return toResponse(savedArticle);
    }

    /**
     * 更新文章 → 清除该文章缓存 + 列表缓存
     * @CacheEvict 保证缓存一致性（Cache Aside 写策略：先更新DB，再删缓存）
     */
    @Transactional
    public ArticleResponse updateArticle(Long id, ArticleRequest request, User currentUser) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) 
            && currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("无权修改此文章");
        }

        boolean previouslyPublished = Boolean.TRUE.equals(article.getPublished());
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

        Article savedArticle = articleRepository.save(article);
        publicationEvents.publishEvent(new ArticlePublicationChanged(savedArticle, previouslyPublished, false));
        return toResponse(savedArticle);
    }

    /**
     * 删除文章 → 清除所有相关缓存
     */
    @Transactional
    public void deleteArticle(Long id, User currentUser) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) 
            && currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("无权删除此文章");
        }

        articleRepository.delete(article);
        publicationEvents.publishEvent(new ArticlePublicationChanged(article, Boolean.TRUE.equals(article.getPublished()), true));
    }

    public ArticleResponse toResponse(Article article) {
        // 点赞数优先从 Redis 读取
        int likeCount = article.getLikeCount();
        try {
            String redisCount = stringRedisTemplate.opsForValue().get(
                    RedisKeyPrefix.ARTICLE_LIKE_COUNT + article.getId()
            );
            if (redisCount != null) {
                likeCount = Integer.parseInt(redisCount);
            }
        } catch (Exception e) {
            // fallback to DB value
        }

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
                .likeCount(likeCount)
                .commentCount(commentRepository.countByArticle(article))
                .published(article.getPublished())
                .featured(article.getFeatured())
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    public ArticleSummaryResponse toSummaryResponse(Article article) {
        int contentLength = article.getContent() == null ? 0 : article.getContent().length();
        int readTimeMinutes = Math.max(1, (int) Math.ceil(contentLength / 300.0));

        return ArticleSummaryResponse.builder()
                .id(article.getId())
                .title(article.getTitle())
                .summary(article.getSummary())
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
                // 摘要接口不访问 Redis 或评论表，避免列表请求产生 N+1 查询。
                .likeCount(article.getLikeCount())
                .published(article.getPublished())
                .featured(article.getFeatured())
                .readTimeMinutes(readTimeMinutes)
                .publishedAt(article.getPublishedAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .build();
    }

    /**
     * 获取文章归档（按年-月分组）
     * 只返回已发布文章，按时间倒序
     */
    public ArchiveResponse getArchive() {
        // The archive projection is small and authoritative; do not revive old titles after withdrawal/republication.
        return buildArchive();
    }

    private ArchiveResponse buildArchive() {
        log.debug("加载已发布文章归档");
        List<ArchiveArticleProjection> articles = articleRepository.findPublishedArchiveRows();

        String[] monthNames = {"", "一月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "十二月"};
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM-dd");

        // 按年分组，保持倒序
        Map<Integer, List<ArchiveArticleProjection>> yearMap = new LinkedHashMap<>();
        for (ArchiveArticleProjection article : articles) {
            LocalDateTime displayTime = article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreatedAt();
            int year = displayTime.getYear();
            yearMap.computeIfAbsent(year, k -> new ArrayList<>()).add(article);
        }

        List<ArchiveResponse.YearArchive> yearArchives = new ArrayList<>();
        for (Map.Entry<Integer, List<ArchiveArticleProjection>> yearEntry : yearMap.entrySet()) {
            int year = yearEntry.getKey();
            List<ArchiveArticleProjection> yearArticles = yearEntry.getValue();

            // 按月分组
            Map<Integer, List<ArchiveArticleProjection>> monthMap = new LinkedHashMap<>();
            for (ArchiveArticleProjection article : yearArticles) {
                LocalDateTime displayTime = article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreatedAt();
                int month = displayTime.getMonthValue();
                monthMap.computeIfAbsent(month, k -> new ArrayList<>()).add(article);
            }

            List<ArchiveResponse.MonthArchive> monthArchives = new ArrayList<>();
            for (Map.Entry<Integer, List<ArchiveArticleProjection>> monthEntry : monthMap.entrySet()) {
                int month = monthEntry.getKey();
                List<ArchiveResponse.ArticleBrief> briefs = monthEntry.getValue().stream()
                        .map(a -> ArchiveResponse.ArticleBrief.builder()
                                .id(a.getId())
                                .title(a.getTitle())
                                .date((a.getPublishedAt() != null ? a.getPublishedAt() : a.getCreatedAt()).format(dayFormatter))
                                .category(a.getCategoryName())
                                .build())
                        .collect(Collectors.toList());

                monthArchives.add(ArchiveResponse.MonthArchive.builder()
                        .month(month)
                        .monthName(monthNames[month])
                        .articles(briefs)
                        .build());
            }

            yearArchives.add(ArchiveResponse.YearArchive.builder()
                    .year(year)
                    .count(yearArticles.size())
                    .months(monthArchives)
                    .build());
        }

        return ArchiveResponse.builder()
                .totalCount(articles.size())
                .years(yearArchives)
                .build();
    }

    // ========== 一人一赞系统 ==========

    /**
     * Toggle 点赞/取消（Redisson 分布式锁 + Lua 原子操作）
     * visitorId: 登录用户为 userId，游客为 anon:ipHash
     */
    public LikeResponseDTO toggleLike(Long articleId, String visitorId) {
        RLock lock = redissonClient.getLock(RedisKeyPrefix.LOCK_LIKE + visitorId);
        boolean isLocked = false;
        try {
            isLocked = lock.tryLock(1, 5, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("操作太频繁，请稍后重试");
            }

            List<Long> result = stringRedisTemplate.execute(
                    LIKE_TOGGLE_SCRIPT,
                    Arrays.asList(
                            RedisKeyPrefix.ARTICLE_LIKED + articleId,
                            RedisKeyPrefix.ARTICLE_LIKE_COUNT + articleId
                    ),
                    visitorId
            );

            LikeResponseDTO dto = new LikeResponseDTO();
            dto.setLiked(result != null && result.get(0) == 1L);
            dto.setLikeCount(result != null ? result.get(1).intValue() : 0);
            return dto;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("点赞操作被中断");
        } finally {
            if (isLocked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 查询当前访客是否已赞
     */
    public boolean isLiked(Long articleId, String visitorId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(
                        RedisKeyPrefix.ARTICLE_LIKED + articleId,
                        visitorId
                )
        );
    }

    /**
     * 获取文章点赞数（优先 Redis，fallback DB）
     */
    public int getLikeCount(Long articleId) {
        String count = stringRedisTemplate.opsForValue().get(
                RedisKeyPrefix.ARTICLE_LIKE_COUNT + articleId
        );
        if (count != null) {
            return Integer.parseInt(count);
        }
        return articleRepository.findById(articleId)
                .map(Article::getLikeCount)
                .orElse(0);
    }

    // ========== UV统计与热门榜 ==========

    /**
     * 获取本周热门文章（基于 UV 排行）
     */
    public List<ArticleSummaryResponse> getWeeklyHotArticles(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeyPrefix.ARTICLE_HOT_WEEKLY, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            // fallback: 降级到按 viewCount 排序
            return self.getPopularArticles(limit);
        }

        List<ArticleSummaryResponse> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long articleId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
            try {
                ArticleSummaryResponse response = articleRepository.findById(articleId)
                        .filter(article -> Boolean.TRUE.equals(article.getPublished()))
                        .map(this::toSummaryResponse)
                        .orElseThrow();
                response.setUvCount(tuple.getScore() != null ? tuple.getScore().longValue() : 0L);
                result.add(response);
            } catch (Exception e) {
                // 文章可能已删除，跳过
            }
        }
        return result;
    }

    // ========== UV 指纹辅助方法 ==========

    /** 获取当前登录用户（可能为 null） */
    private User getCurrentUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof User) {
                return (User) auth.getPrincipal();
            }
        } catch (Exception ignored) {}
        return null;
    }

    /** 生成访客指纹（登录用户用 userId，匿名用户用 IP+UA 的 MD5） */
    private String generateFingerprint(HttpServletRequest request, User user) {
        if (user != null) {
            return "u:" + user.getId();
        }
        String ip = getClientIp(request);
        String ua = Optional.ofNullable(request.getHeader("User-Agent")).orElse("unknown");
        return "a:" + DigestUtils.md5DigestAsHex((ip + ":" + ua).getBytes());
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
