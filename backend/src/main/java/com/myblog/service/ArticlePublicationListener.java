package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.redis.CacheClient;
import com.myblog.repository.ArticleRepository;

import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.concurrent.Executor;

@Component
@Slf4j
public class ArticlePublicationListener {
    private final CacheClient cacheClient;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final FeedService feedService;
    private final ArticleRepository articleRepository;
    private final Executor projections;

    public ArticlePublicationListener(CacheClient cacheClient, CacheManager cacheManager,
            StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient,
            FeedService feedService, ArticleRepository articleRepository,
            @Qualifier("articleProjectionExecutor") Executor projections) {
        this.cacheClient = cacheClient;
        this.cacheManager = cacheManager;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
        this.feedService = feedService;
        this.articleRepository = articleRepository;
        this.projections = projections;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(ArticlePublicationChanged change) {
        try {
            projections.execute(() -> attempt(() -> updateProjections(change)));
        } catch (RuntimeException rejected) {
            // This bounded, in-memory queue is best effort. Public reads independently verify DB state.
            log.warn("Article projection queue unavailable; committed article {} remains saved", change.articleId(), rejected);
        }
    }

    private void updateProjections(ArticlePublicationChanged change) {
        Long id = change.articleId();
        attempt(() -> cacheClient.evictArticle(id));
        for (String name :
                List.of(
                        "articleDetail",
                        "featuredArticleSummaries",
                        "popularArticleSummaries",
                        "articleArchiveSummary",
                        "featuredArticles",
                        "popularArticles",
                        "dashboardStats")) {
            attempt(
                    () -> {
                        Cache cache = cacheManager.getCache(name);
                        if (cache != null) cache.clear();
                    });
        }
        if ((change.deleted() || !change.published())
                && !articleRepository.existsByIdAndPublishedTrue(id)) {
            attempt(
                    () ->
                            stringRedisTemplate
                                    .opsForZSet()
                                    .remove(RedisKeyPrefix.ARTICLE_HOT_WEEKLY, id.toString()));
        }
        if (change.deleted()) {
            attempt(
                    () ->
                            stringRedisTemplate.delete(
                                    List.of(
                                            RedisKeyPrefix.ARTICLE_LIKED + id,
                                            RedisKeyPrefix.ARTICLE_LIKE_COUNT + id,
                                            RedisKeyPrefix.ARTICLE_UV + id)));
        } else if (change.published() && !change.previouslyPublished()
                && articleRepository.existsByIdAndPublishedTrue(id)) {
            attempt(
                    () ->
                            redissonClient
                                    .<Long>getBloomFilter(RedisKeyPrefix.BLOOM_ARTICLE_ID)
                                    .add(id));
            attempt(() -> feedService.pushToFollowers(id, change.publishedAt(), articleRepository.findPublishedTagIds(id)));
        }
    }

    private void attempt(Runnable operation) {
        try {
            operation.run();
        } catch (RuntimeException e) {
            // Public reads independently enforce publication in the database, even if cache
            // eviction fails.
            log.warn(
                    "Article post-commit projection update failed; database visibility remains"
                        + " authoritative",
                    e);
        }
    }
}
