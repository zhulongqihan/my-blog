package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.common.redis.CacheClient;
import com.myblog.entity.Article;

import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;

@SpringJUnitConfig(PublicationCommitTest.Config.class)
class PublicationCommitTest {
    @Configuration
    @EnableTransactionManagement
    @Import(ArticlePublicationListener.class)
    static class Config {
        @Bean
        CacheClient cacheClient() {
            return mock(CacheClient.class);
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }

        @Bean
        StringRedisTemplate strings() {
            return mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        }

        @Bean
        RedissonClient redisson() {
            return mock(RedissonClient.class, RETURNS_DEEP_STUBS);
        }

        @Bean
        FeedService feeds() {
            return mock(FeedService.class);
        }

        @Bean
        PlatformTransactionManager transactions() {
            return new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() {
                    return new Object();
                }

                protected void doBegin(Object t, TransactionDefinition d) {}

                protected void doCommit(DefaultTransactionStatus s) {}

                protected void doRollback(DefaultTransactionStatus s) {}
            };
        }
    }

    @Autowired ApplicationEventPublisher events;
    @Autowired CacheClient cache;
    @Autowired CacheManager caches;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void clear() {
        reset(cache);
        caches.getCache("featuredArticleSummaries").clear();
        caches.getCache("articleArchiveSummary").clear();
    }

    @Test
    void allPublicProjectionsAreInvalidatedAfterCommitNotBefore() {
        caches.getCache("featuredArticleSummaries").put("all", "stale");
        caches.getCache("articleArchiveSummary").put("all", "stale");
        new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            events.publishEvent(
                                    new ArticlePublicationChanged(
                                            Article.builder().id(7L).published(false).build(),
                                            true,
                                            false));
                            verifyNoInteractions(cache);
                            assertThat(caches.getCache("featuredArticleSummaries").get("all"))
                                    .isNotNull();
                            return null;
                        });
        verify(cache).evictArticle(7L);
        assertThat(caches.getCache("featuredArticleSummaries").get("all")).isNull();
        assertThat(caches.getCache("articleArchiveSummary").get("all")).isNull();
    }

    @Test
    void rollbackDoesNotEvictOrPushPublication() {
        new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            events.publishEvent(
                                    new ArticlePublicationChanged(
                                            Article.builder().id(7L).published(true).build(),
                                            false,
                                            false));
                            status.setRollbackOnly();
                            return null;
                        });
        verifyNoInteractions(cache);
    }
}
