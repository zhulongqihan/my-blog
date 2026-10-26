package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.common.redis.CacheClient;
import com.myblog.entity.Article;
import com.myblog.repository.ArticleRepository;

import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;
import java.util.concurrent.*;

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
        ArticleRepository articles() {
            return mock(ArticleRepository.class);
        }

        @Bean(destroyMethod = "shutdownNow")
        ExecutorService articleProjectionExecutor() {
            return Executors.newSingleThreadExecutor();
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
    @Autowired @Qualifier("articleProjectionExecutor") ExecutorService projections;

    @BeforeEach
    void clear() throws Exception {
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        reset(cache);
        caches.getCache("featuredArticleSummaries").clear();
        caches.getCache("articleArchiveSummary").clear();
    }

    @Test
    void allPublicProjectionsAreInvalidatedAfterCommitNotBefore() throws Exception {
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
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verify(cache).evictArticle(7L);
        assertThat(caches.getCache("featuredArticleSummaries").get("all")).isNull();
        assertThat(caches.getCache("articleArchiveSummary").get("all")).isNull();
    }

    @Test
    void rollbackDoesNotEvictOrPushPublication() throws Exception {
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
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verifyNoInteractions(cache);
    }
}
