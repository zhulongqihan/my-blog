package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.entity.Article;
import com.myblog.repository.*;
import com.myblog.task.ViewCountSyncTask;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.*;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

@DataJpaTest(
        properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@Import(ViewCountBatchApplier.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ViewCountRecoveryTest {
    @Autowired ArticleRepository articles;
    @Autowired AppliedViewCountBatchRepository batches;
    @Autowired ViewCountBatchApplier applier;
    @Autowired PlatformTransactionManager transactionManager;
    LettuceConnectionFactory factory;
    StringRedisTemplate redis;
    ViewCountBuffer buffer;
    Set<Long> createdIds = new HashSet<>();

    @BeforeAll
    void redis() {
        String port =
                System.getProperty("blog.test.redis.port", System.getenv("BLOG_TEST_REDIS_PORT"));
        Assumptions.assumeTrue(
                port != null && !port.isBlank(),
                "An explicit isolated BLOG_TEST_REDIS_PORT is required");
        var config = new RedisStandaloneConfiguration("127.0.0.1", Integer.parseInt(port));
        config.setDatabase(8);
        factory = new LettuceConnectionFactory(config);
        factory.afterPropertiesSet();
        factory.start();
        redis = new StringRedisTemplate(factory);
        buffer = new ViewCountBuffer(redis);
    }

    @BeforeEach
    void clean() {
        for (Long id : createdIds)
            redis.delete(List.of(active(id), ViewCountBuffer.PENDING_PREFIX + id));
        createdIds.clear();
        batches.deleteAll();
        articles.deleteAll();
    }

    @AfterAll
    void close() {
        if (redis != null)
            for (Long id : createdIds)
                redis.delete(List.of(active(id), ViewCountBuffer.PENDING_PREFIX + id));
        if (factory != null) factory.destroy();
    }

    private Article article() {
        var a =
                articles.saveAndFlush(
                        Article.builder()
                                .title("test")
                                .published(true)
                                .viewCount(10)
                                .content("body")
                                .build());
        createdIds.add(a.getId());
        return a;
    }

    private String active(Long id) {
        return com.myblog.common.constant.RedisKeyPrefix.ARTICLE_VIEW_COUNT + id;
    }

    private int count(Long id) {
        return articles.findById(id).orElseThrow().getViewCount();
    }

    @Test
    void captureCanBeRecoveredAfterProcessRestartBeforeDatabaseCommit() {
        var a = article();
        redis.opsForValue().set(active(a.getId()), "3");
        var captured = buffer.capture(a.getId()).orElseThrow();
        redis.opsForValue().increment(active(a.getId()), 2);
        var restarted = new ViewCountBuffer(redis);
        assertThat(restarted.capture(a.getId()).orElseThrow()).isEqualTo(captured);
        applier.apply(captured);
        restarted.acknowledge(captured);
        var later = restarted.capture(a.getId()).orElseThrow();
        applier.apply(later);
        restarted.acknowledge(later);
        assertThat(count(a.getId())).isEqualTo(15);
        assertThat(batches.count()).isEqualTo(2);
    }

    @Test
    void committedDatabaseBatchSurvivesAcknowledgementFailureWithoutDoubleCounting() {
        var a = article();
        redis.opsForValue().set(active(a.getId()), "3");
        var failing = spy(buffer);
        doThrow(new IllegalStateException("simulated lost ACK")).when(failing).acknowledge(any());
        new ViewCountSyncTask(failing, applier).syncViewCounts();
        assertThat(count(a.getId())).isEqualTo(13);
        assertThat(redis.hasKey(ViewCountBuffer.PENDING_PREFIX + a.getId())).isTrue();
        redis.opsForValue().increment(active(a.getId()), 4);
        var recovered = new ViewCountSyncTask(new ViewCountBuffer(redis), applier);
        recovered.syncViewCounts();
        assertThat(count(a.getId())).isEqualTo(13);
        assertThat(redis.opsForValue().get(active(a.getId()))).isEqualTo("4");
        recovered.syncViewCounts();
        assertThat(count(a.getId())).isEqualTo(17);
        assertThat(batches.count()).isEqualTo(2);
    }

    @Test
    void rollbackAfterCounterUpdateDoesNotConsumeBatch() {
        var a = article();
        redis.opsForValue().set(active(a.getId()), "3");
        var batch = buffer.capture(a.getId()).orElseThrow();
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactionManager)
                                        .execute(
                                                status -> {
                                                    applier.apply(batch);
                                                    throw new IllegalStateException(
                                                            "simulated crash before commit");
                                                }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(count(a.getId())).isEqualTo(10);
        assertThat(batches.count()).isZero();
        assertThat(buffer.capture(a.getId()).orElseThrow()).isEqualTo(batch);
        applier.apply(batch);
        buffer.acknowledge(batch);
        assertThat(count(a.getId())).isEqualTo(13);
    }

    @Test
    void failedDatabaseAttemptLeavesPendingBatchForRetry() {
        var a = article();
        redis.opsForValue().set(active(a.getId()), "2");
        var failing = mock(ViewCountBatchApplier.class);
        doThrow(new IllegalStateException("DB unavailable")).when(failing).apply(any());
        new ViewCountSyncTask(buffer, failing).syncViewCounts();
        assertThat(count(a.getId())).isEqualTo(10);
        assertThat(buffer.capture(a.getId())).isPresent();
        new ViewCountSyncTask(buffer, applier).syncViewCounts();
        assertThat(count(a.getId())).isEqualTo(12);
    }

    @Test
    void oldAcknowledgementCannotDeleteANewerBatch() {
        var a = article();
        redis.opsForValue().set(active(a.getId()), "2");
        var first = buffer.capture(a.getId()).orElseThrow();
        applier.apply(first);
        buffer.acknowledge(first);
        redis.opsForValue().set(active(a.getId()), "3");
        var second = buffer.capture(a.getId()).orElseThrow();
        buffer.acknowledge(first);
        assertThat(buffer.capture(a.getId()).orElseThrow()).isEqualTo(second);
        applier.apply(second);
        buffer.acknowledge(second);
        assertThat(count(a.getId())).isEqualTo(15);
    }

    @Test
    void legacyJsonNumericCounterIsCompatible() {
        var a = article();
        var legacy = new RedisTemplate<String, Object>();
        legacy.setConnectionFactory(factory);
        legacy.setKeySerializer(new StringRedisSerializer());
        legacy.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        legacy.afterPropertiesSet();
        legacy.opsForValue().set(active(a.getId()), 7L);
        var batch = buffer.capture(a.getId()).orElseThrow();
        assertThat(batch.delta()).isEqualTo(7L);
        applier.apply(batch);
        buffer.acknowledge(batch);
        assertThat(count(a.getId())).isEqualTo(17);
    }

    @Test
    void concurrentWritersAndTwoSyncWorkersDoNotLoseOrRepeatIncrements() throws Exception {
        var a = article();
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<?>> jobs = new ArrayList<>();
            for (int worker = 0; worker < 4; worker++)
                jobs.add(
                        pool.submit(
                                () -> {
                                    for (int i = 0; i < 30; i++)
                                        redis.opsForValue().increment(active(a.getId()));
                                }));
            for (int worker = 0; worker < 2; worker++)
                jobs.add(
                        pool.submit(
                                () -> {
                                    for (int i = 0; i < 8; i++)
                                        new ViewCountSyncTask(buffer, applier).syncViewCounts();
                                }));
            for (Future<?> job : jobs) job.get(60, TimeUnit.SECONDS);
            var sync = new ViewCountSyncTask(buffer, applier);
            for (int i = 0; i < 4; i++) sync.syncViewCounts();
            assertThat(count(a.getId())).isEqualTo(130);
            assertThat(redis.hasKey(active(a.getId()))).isFalse();
            assertThat(redis.hasKey(ViewCountBuffer.PENDING_PREFIX + a.getId())).isFalse();
        } finally {
            pool.shutdownNow();
        }
    }
}
