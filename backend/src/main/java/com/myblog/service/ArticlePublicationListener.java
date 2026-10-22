package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.redis.CacheClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePublicationListener {
    private final CacheClient cacheClient;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final FeedService feedService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void afterCommit(ArticlePublicationChanged change) {
        Long id = change.article().getId();
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
        if (change.deleted() || !Boolean.TRUE.equals(change.article().getPublished())) {
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
        } else if (Boolean.TRUE.equals(change.article().getPublished())
                && !change.previouslyPublished()) {
            attempt(
                    () ->
                            redissonClient
                                    .<Long>getBloomFilter(RedisKeyPrefix.BLOOM_ARTICLE_ID)
                                    .add(id));
            attempt(() -> feedService.pushToFollowers(change.article()));
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
