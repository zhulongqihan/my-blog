package com.myblog.common.redis;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.dto.ArticleResponse;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

class CacheUnavailableTest {
    @Test
    void optionalCacheFailureStillLoadsThePublishedDatabaseResult() {
        var redis = mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS);
        when(redis.opsForValue().get(anyString()))
                .thenThrow(new RedisConnectionFailureException("isolated cache down"));
        var cache = new CacheClient(redis, mock(RedissonClient.class));
        try {
            var value = ArticleResponse.builder().id(7L).content("published").build();
            assertThat(
                            cache.queryWithLogicalExpire(
                                    "cache:",
                                    7L,
                                    ArticleResponse.class,
                                    id -> value,
                                    30L,
                                    TimeUnit.MINUTES))
                    .isSameAs(value);
            assertThat(
                            cache.queryWithMutex(
                                    "cache:",
                                    7L,
                                    ArticleResponse.class,
                                    id -> value,
                                    30L,
                                    TimeUnit.MINUTES))
                    .isSameAs(value);
        } finally {
            cache.close();
        }
    }
}
