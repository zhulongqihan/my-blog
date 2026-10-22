package com.myblog.common.redis;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.dto.ArticleResponse;

import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CacheLockIntegrationTest {
    LettuceConnectionFactory factory;
    StringRedisTemplate redis;
    CacheClient client;
    String prefix = "test:phase1:cache:" + UUID.randomUUID() + ":";

    @BeforeAll
    void connect() {
        String port =
                System.getProperty("blog.test.redis.port", System.getenv("BLOG_TEST_REDIS_PORT"));
        Assumptions.assumeTrue(
                port != null && !port.isBlank(),
                "An explicit isolated BLOG_TEST_REDIS_PORT is required");
        var c = new RedisStandaloneConfiguration("127.0.0.1", Integer.parseInt(port));
        c.setDatabase(9);
        factory = new LettuceConnectionFactory(c);
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        client = new CacheClient(redis, mock(RedissonClient.class));
    }

    @AfterAll
    void close() {
        if (client != null) client.close();
        if (factory != null) factory.destroy();
    }

    @Test
    void expiredOwnersReleaseDoesNotDeleteTheNewOwnersLock() throws Exception {
        String key = prefix + "owner";
        String first = client.tryLock(key);
        assertThat(first).isNotNull();
        redis.expire(key, 20, TimeUnit.MILLISECONDS);
        Thread.sleep(70);
        String second = client.tryLock(key);
        assertThat(second).isNotNull().isNotEqualTo(first);
        client.unlock(key, first);
        assertThat(redis.opsForValue().get(key)).isEqualTo(second);
        client.unlock(key, second);
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void nonHolderCannotReleaseLock() {
        String key = prefix + "nonholder";
        String token = client.tryLock(key);
        client.unlock(key, null);
        assertThat(redis.opsForValue().get(key)).isEqualTo(token);
        client.unlock(key, "wrong");
        assertThat(redis.opsForValue().get(key)).isEqualTo(token);
        client.unlock(key, token);
    }

    @Test
    void competingReadersRebuildOnceAndGetTheSameValue() throws Exception {
        String cachePrefix = prefix + "read:";
        AtomicInteger loads = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<ArticleResponse>> results = new ArrayList<>();
            for (int i = 0; i < 4; i++)
                results.add(
                        pool.submit(
                                () ->
                                        client.queryWithMutex(
                                                cachePrefix,
                                                1L,
                                                ArticleResponse.class,
                                                id -> {
                                                    loads.incrementAndGet();
                                                    try {
                                                        Thread.sleep(60);
                                                    } catch (InterruptedException e) {
                                                        Thread.currentThread().interrupt();
                                                    }
                                                    return ArticleResponse.builder()
                                                            .id(1L)
                                                            .content("body")
                                                            .build();
                                                },
                                                10L,
                                                TimeUnit.MINUTES)));
            for (var result : results)
                assertThat(result.get(20, TimeUnit.SECONDS).getContent()).isEqualTo("body");
            assertThat(loads).hasValue(1);
        } finally {
            pool.shutdownNow();
            redis.delete(cachePrefix + 1);
        }
    }

    @Test
    void failedLoaderReleasesOnlyItsOwnLock() {
        String cachePrefix = prefix + "failed:";
        assertThatThrownBy(
                        () ->
                                client.queryWithMutex(
                                        cachePrefix,
                                        1L,
                                        ArticleResponse.class,
                                        id -> {
                                            throw new IllegalStateException("DB failed");
                                        },
                                        10L,
                                        TimeUnit.MINUTES))
                .isInstanceOf(IllegalStateException.class);
        assertThat(
                        redis.hasKey(
                                com.myblog.common.constant.RedisKeyPrefix.LOCK_CACHE_REBUILD
                                        + cachePrefix
                                        + 1))
                .isFalse();
    }

    @Test
    void cachedPublicationTimestampKeepsDatabasePrecision() {
        String cachePrefix = prefix + "version:";
        var timestamp = java.time.LocalDateTime.of(2026, 10, 22, 12, 0, 0, 123456000);
        var value =
                ArticleResponse.builder()
                        .id(1L)
                        .published(true)
                        .updatedAt(timestamp)
                        .content("body")
                        .build();
        client.set(cachePrefix + 1L, value, 10L, TimeUnit.MINUTES);
        var cached =
                client.queryWithMutex(
                        cachePrefix,
                        1L,
                        ArticleResponse.class,
                        id -> {
                            throw new AssertionError("must be a cache hit");
                        },
                        10L,
                        TimeUnit.MINUTES);
        assertThat(cached.getUpdatedAt()).isEqualTo(timestamp);
        redis.delete(cachePrefix + 1L);
    }

    @Test
    void logicalCachePreservesPublicationTimestampAndStillServesCurrentVersion() {
        String cachePrefix = prefix + "logical-version:";
        var timestamp = java.time.LocalDateTime.of(2026, 10, 22, 12, 0, 0, 987654000);
        var value =
                ArticleResponse.builder()
                        .id(1L)
                        .published(true)
                        .updatedAt(timestamp)
                        .content("body")
                        .build();
        client.setWithLogicalExpire(cachePrefix + 1L, value, 30L, TimeUnit.MINUTES);
        var cached =
                client.queryWithLogicalExpire(
                        cachePrefix,
                        1L,
                        ArticleResponse.class,
                        id -> {
                            throw new AssertionError("current logical cache must hit");
                        },
                        30L,
                        TimeUnit.MINUTES);
        assertThat(cached.getUpdatedAt()).isEqualTo(timestamp);
        redis.delete(cachePrefix + 1L);
    }
}
