package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.config.RedisConfig;
import com.myblog.repository.*;
import com.myblog.service.admin.DashboardService;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Explicit local disposable Redis only; no database or production services. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DashboardRedisPipelineIntegrationTest {
    LettuceConnectionFactory factory;
    RedisTemplate<String, Object> redis;
    DashboardService dashboard;
    List<String> keys;

    @BeforeAll void connect() {
        String configuredPort = System.getProperty("blog.test.redis.port", System.getenv("BLOG_TEST_REDIS_PORT"));
        Assumptions.assumeTrue(configuredPort != null && !configuredPort.isBlank(),
                "An explicit isolated BLOG_TEST_REDIS_PORT is required");
        var configuration = new RedisStandaloneConfiguration("127.0.0.1", Integer.parseInt(configuredPort));
        configuration.setDatabase(15);
        factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet(); factory.start();
        redis = new RedisConfig().redisTemplate(factory);
        dashboard = new DashboardService(mock(ArticleRepository.class), mock(CommentRepository.class),
                mock(UserRepository.class), mock(CategoryRepository.class), redis, mock(StringRedisTemplate.class));
        LocalDate today = LocalDate.now();
        keys = IntStream.range(0,7).mapToObj(i -> RedisKeyPrefix.DAILY_VIEW_COUNT + today.minusDays(6-i)).toList();
    }

    @BeforeEach void clearOwnFixture() { redis.delete(keys); }

    @AfterAll void cleanup() {
        if (redis != null) redis.delete(keys);
        if (factory != null) factory.destroy();
    }

    @Test void pipelineKeepsJsonCountersAndMissingDaysInOrder() {
        redis.opsForValue().set(keys.get(0), 3L);
        redis.opsForValue().set(keys.get(6), 9L);
        var result = dashboard.getStats();
        assertThat(result.getViewTrend()).extracting("count").containsExactly(3L,0L,0L,0L,0L,0L,9L);
        assertThat(result.getTodayViews()).isEqualTo(9L);
    }

    @Test void wrongRedisTypeCannotBecomeAZeroViewCount() {
        redis.opsForSet().add(keys.get(6), "wrong type");
        assertThatThrownBy(dashboard::getStats).isInstanceOf(org.springframework.dao.DataAccessException.class);
    }
}
