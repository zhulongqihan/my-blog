package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.exception.BusinessException;
import com.myblog.common.redis.CacheClient;
import com.myblog.dto.ArchiveResponse;
import com.myblog.dto.ArticleRequest;
import com.myblog.dto.ArticleResponse;
import com.myblog.dto.LikeResponseDTO;
import com.myblog.entity.*;
import com.myblog.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
     * 获取文章详情 — 缓存三重防御
     *
     * 改造：移除 @Cacheable，改用 CacheClient 手动查询
     * ① 精选文章 → 逻辑过期方案（高可用，零延迟）
     * ② 普通文章 → 布隆过滤器 + 互斥锁方案（强一致性）
     */
    public ArticleResponse getArticle(Long id) {
        // 先尝试逻辑过期方案（精选/热门文章已预热）
        ArticleResponse result = cacheClient.queryWithLogicalExpire(
                RedisKeyPrefix.ARTICLE_DETAIL_LOGIC, id, ArticleResponse.class,
                this::getArticleFromDb, 30L, TimeUnit.MINUTES
        );
        if (result != null) return result;

        // 普通文章走布隆过滤器 + 互斥锁方案
        result = cacheClient.queryWithBloomFilter(
                RedisKeyPrefix.ARTICLE_DETAIL, id, ArticleResponse.class,
                this::getArticleFromDb, 30L, TimeUnit.MINUTES
        );

        if (result == null) {
            throw new RuntimeException("文章不存在: " + id);
        }
        return result;
    }

    /** 从 DB 加载文章详情（供 CacheClient 回调使用） */
    private ArticleResponse getArticleFromDb(Long id) {
        return articleRepository.findById(id)
                .map(this::toResponse)
                .orElse(null);
    }

    /**
     * 获取文章详情 + 递增浏览量 + 记录 UV
     *
     * PV：Redis INCR（原子计数器）
     * UV：HyperLogLog PFADD（概率去重，12KB/文章）
     */
    public ArticleResponse getArticleAndIncrementView(Long id, HttpServletRequest request) {
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
        ArticleResponse response = self.getArticle(id);

        // ⑤ 附加 UV 数
        Long uvCount = stringRedisTemplate.opsForHyperLogLog().size(
                RedisKeyPrefix.ARTICLE_UV + id
        );
        response.setUvCount(uvCount != null ? uvCount : 0L);

        return response;
    }

    /** 向下兼容：无 request 参数的 getArticleAndIncrementView */
    public ArticleResponse getArticleAndIncrementView(Long id) {
        String viewKey = RedisKeyPrefix.ARTICLE_VIEW_COUNT + id;
        redisTemplate.opsForValue().increment(viewKey);
        String todayKey = RedisKeyPrefix.DAILY_VIEW_COUNT
                + LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        redisTemplate.opsForValue().increment(todayKey);
        return self.getArticle(id);
    }

    /**
     * 创建文章 → 清除列表缓存（精选/热门可能变化）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "featuredArticles", allEntries = true),
        @CacheEvict(value = "popularArticles", allEntries = true),
        @CacheEvict(value = "articleArchive", allEntries = true)
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

        Article savedArticle = articleRepository.save(article);

        // 同步布隆过滤器
        if (Boolean.TRUE.equals(savedArticle.getPublished())) {
            try {
                RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyPrefix.BLOOM_ARTICLE_ID);
                bloomFilter.add(savedArticle.getId());
            } catch (Exception e) {
                log.warn("布隆过滤器同步失败: {}", e.getMessage());
            }

            // 推送到关注者 Feed
            try {
                feedService.pushToFollowers(savedArticle);
            } catch (Exception e) {
                log.warn("Feed 推送失败: {}", e.getMessage());
            }
        }

        return toResponse(savedArticle);
    }

    /**
     * 更新文章 → 清除该文章缓存 + 列表缓存
     * @CacheEvict 保证缓存一致性（Cache Aside 写策略：先更新DB，再删缓存）
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "articleDetail", key = "#id"),
        @CacheEvict(value = "featuredArticles", allEntries = true),
        @CacheEvict(value = "popularArticles", allEntries = true),
        @CacheEvict(value = "articleArchive", allEntries = true)
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
        @CacheEvict(value = "popularArticles", allEntries = true),
        @CacheEvict(value = "articleArchive", allEntries = true)
    })
    public void deleteArticle(Long id, User currentUser) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("文章不存在"));

        if (!article.getAuthor().getId().equals(currentUser.getId()) 
            && currentUser.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("无权删除此文章");
        }

        articleRepository.delete(article);

        // 清理 Redis 相关 Key
        stringRedisTemplate.delete(RedisKeyPrefix.ARTICLE_LIKED + id);
        stringRedisTemplate.delete(RedisKeyPrefix.ARTICLE_LIKE_COUNT + id);
        stringRedisTemplate.delete(RedisKeyPrefix.ARTICLE_UV + id);
        stringRedisTemplate.delete(RedisKeyPrefix.ARTICLE_DETAIL + id);
        stringRedisTemplate.delete(RedisKeyPrefix.ARTICLE_DETAIL_LOGIC + id);
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

    /**
     * 获取文章归档（按年-月分组）
     * 只返回已发布文章，按时间倒序
     */
    @Cacheable(value = "articleArchive", key = "'all'")
    public ArchiveResponse getArchive() {
        log.info("[Cache MISS] 文章归档 - 从数据库加载");
        List<Article> articles = articleRepository.findByPublishedTrueOrderByPublishedAtDesc();

        String[] monthNames = {"", "一月", "二月", "三月", "四月", "五月", "六月",
                "七月", "八月", "九月", "十月", "十一月", "十二月"};
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("MM-dd");

        // 按年分组，保持倒序
        Map<Integer, List<Article>> yearMap = new LinkedHashMap<>();
        for (Article article : articles) {
            LocalDateTime displayTime = article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreatedAt();
            int year = displayTime.getYear();
            yearMap.computeIfAbsent(year, k -> new ArrayList<>()).add(article);
        }

        List<ArchiveResponse.YearArchive> yearArchives = new ArrayList<>();
        for (Map.Entry<Integer, List<Article>> yearEntry : yearMap.entrySet()) {
            int year = yearEntry.getKey();
            List<Article> yearArticles = yearEntry.getValue();

            // 按月分组
            Map<Integer, List<Article>> monthMap = new LinkedHashMap<>();
            for (Article article : yearArticles) {
                LocalDateTime displayTime = article.getPublishedAt() != null ? article.getPublishedAt() : article.getCreatedAt();
                int month = displayTime.getMonthValue();
                monthMap.computeIfAbsent(month, k -> new ArrayList<>()).add(article);
            }

            List<ArchiveResponse.MonthArchive> monthArchives = new ArrayList<>();
            for (Map.Entry<Integer, List<Article>> monthEntry : monthMap.entrySet()) {
                int month = monthEntry.getKey();
                List<ArchiveResponse.ArticleBrief> briefs = monthEntry.getValue().stream()
                        .map(a -> ArchiveResponse.ArticleBrief.builder()
                                .id(a.getId())
                                .title(a.getTitle())
                                .date((a.getPublishedAt() != null ? a.getPublishedAt() : a.getCreatedAt()).format(dayFormatter))
                                .category(a.getCategory() != null ? a.getCategory().getName() : null)
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
    public List<ArticleResponse> getWeeklyHotArticles(int limit) {
        Set<ZSetOperations.TypedTuple<String>> tuples = stringRedisTemplate.opsForZSet()
                .reverseRangeWithScores(RedisKeyPrefix.ARTICLE_HOT_WEEKLY, 0, limit - 1);

        if (tuples == null || tuples.isEmpty()) {
            // fallback: 降级到按 viewCount 排序
            return self.getPopularArticles(limit);
        }

        List<ArticleResponse> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            Long articleId = Long.parseLong(Objects.requireNonNull(tuple.getValue()));
            try {
                ArticleResponse response = self.getArticle(articleId);
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
