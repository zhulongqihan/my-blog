package com.myblog.task;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.redis.CacheClient;
import com.myblog.entity.Article;
import com.myblog.entity.UserTagFollow;
import com.myblog.repository.ArticleRepository;
import com.myblog.repository.UserTagFollowRepository;
import com.myblog.service.ArticleService;
import com.myblog.service.CategoryService;
import com.myblog.service.TagService;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存预热任务
 *
 * 启动时执行：
 * 1. 预热分类/标签/精选/热门缓存（已有）
 * 2. 初始化布隆过滤器（缓存穿透防御）
 * 3. 预热精选文章逻辑过期缓存（缓存击穿防御）
 * 4. 重建关注关系到 Redis（Feed 流冷启动）
 */
@Slf4j
@Component
public class CacheWarmupTask implements ApplicationRunner {

    private final CategoryService categoryService;
    private final TagService tagService;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final RedissonClient redissonClient;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    private final UserTagFollowRepository followRepository;

    public CacheWarmupTask(CategoryService categoryService, TagService tagService,
            ArticleService articleService, ArticleRepository articleRepository,
            RedissonClient redissonClient,
            @Qualifier("stringRedisTemplate") StringRedisTemplate stringRedisTemplate,
            CacheClient cacheClient,
            UserTagFollowRepository followRepository) {
        this.categoryService = categoryService;
        this.tagService = tagService;
        this.articleService = articleService;
        this.articleRepository = articleRepository;
        this.redissonClient = redissonClient;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheClient = cacheClient;
        this.followRepository = followRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("[CacheWarmup] 开始缓存预热...");
        
        try {
            // 预热分类列表
            categoryService.findAll();
            log.info("[CacheWarmup] ✓ 分类列表已预热");
            
            // 预热标签列表
            tagService.findAll();
            log.info("[CacheWarmup] ✓ 标签列表已预热");
            
            // 预热精选文章
            articleService.getFeaturedArticles();
            log.info("[CacheWarmup] ✓ 精选文章已预热");
            
            // 预热热门文章 Top5
            articleService.getPopularArticles(5);
            log.info("[CacheWarmup] ✓ 热门文章已预热");

            // ========== v2.0 新增预热 ==========

            // 初始化布隆过滤器（缓存穿透防御）
            initBloomFilter();

            // 预热精选文章逻辑过期缓存（缓存击穿防御）
            warmupLogicalExpireCache();

            // 重建关注关系到 Redis（Feed 流冷启动）
            rebuildFollowRelations();

            log.info("[CacheWarmup] 缓存预热完成！");
        } catch (Exception e) {
            log.warn("[CacheWarmup] 缓存预热失败（不影响正常使用）: {}", e.getMessage());
        }
    }

    /**
     * 初始化布隆过滤器 — 加载所有已发布文章ID
     */
    private void initBloomFilter() {
        try {
            RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyPrefix.BLOOM_ARTICLE_ID);
            bloomFilter.tryInit(10000L, 0.01); // 预计容量 1万，误判率 1%

            List<Long> allArticleIds = articleRepository.findAllPublishedArticleIds();
            for (Long id : allArticleIds) {
                bloomFilter.add(id);
            }
            log.info("[CacheWarmup] ✓ 布隆过滤器初始化完成，加载 {} 篇文章ID", allArticleIds.size());
        } catch (Exception e) {
            log.warn("[CacheWarmup] 布隆过滤器初始化失败: {}", e.getMessage());
        }
    }

    /**
     * 预热精选文章逻辑过期缓存 — 永不物理过期，异步重建
     */
    private void warmupLogicalExpireCache() {
        try {
            List<Article> featured = articleRepository.findByFeaturedTrueAndPublishedTrue();
            for (Article article : featured) {
                cacheClient.setWithLogicalExpire(
                        RedisKeyPrefix.ARTICLE_DETAIL_LOGIC + article.getId(),
                        articleService.toResponse(article), 30L, TimeUnit.MINUTES
                );
            }
            log.info("[CacheWarmup] ✓ 逻辑过期缓存预热完成，加载 {} 篇精选文章", featured.size());
        } catch (Exception e) {
            log.warn("[CacheWarmup] 逻辑过期缓存预热失败: {}", e.getMessage());
        }
    }

    /**
     * 重建关注关系到 Redis — 保证 Feed 流冷启动后可用
     */
    private void rebuildFollowRelations() {
        try {
            List<UserTagFollow> allFollows = followRepository.findAll();
            for (UserTagFollow f : allFollows) {
                stringRedisTemplate.opsForSet().add(
                        RedisKeyPrefix.FOLLOW_TAGS + f.getUserId(), f.getTagId().toString()
                );
                stringRedisTemplate.opsForSet().add(
                        RedisKeyPrefix.TAG_FOLLOWERS + f.getTagId(), f.getUserId().toString()
                );
            }
            log.info("[CacheWarmup] ✓ 关注关系重建完成: {} 条记录", allFollows.size());
        } catch (Exception e) {
            log.warn("[CacheWarmup] 关注关系重建失败: {}", e.getMessage());
        }
    }
}
