package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myblog.common.annotation.Log;
import com.myblog.common.aspect.LogAspect;
import com.myblog.common.redis.CacheClient;
import com.myblog.config.RabbitMQConfig;
import com.myblog.config.ArticleSideEffectConfig;
import com.myblog.dto.ArticlePublicationVersion;
import com.myblog.entity.Article;
import com.myblog.entity.Tag;
import com.myblog.repository.ArticleRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.*;

@SpringJUnitConfig(ArticleSaveSideEffectTest.Config.class)
class ArticleSaveSideEffectTest {
    @Configuration
    @EnableTransactionManagement
    @Import({ArticlePublicationListener.class, LogAspect.class})
    static class Config {
        @Bean CacheClient cacheClient() { return mock(CacheClient.class); }
        @Bean CacheManager cacheManager() { return new ConcurrentMapCacheManager(); }
        @Bean StringRedisTemplate strings() { return mock(StringRedisTemplate.class, RETURNS_DEEP_STUBS); }
        @Bean RedissonClient redisson() { return mock(RedissonClient.class, RETURNS_DEEP_STUBS); }
        @Bean FeedService feeds() { return mock(FeedService.class); }
        @Bean ArticleRepository articles() { return mock(ArticleRepository.class); }
        @Bean RabbitTemplate rabbit() { return mock(RabbitTemplate.class); }
        @Bean MQProducerService producer(RabbitTemplate rabbit) { return new MQProducerService(rabbit); }
        @Bean OperationLogService operationLogs() { return mock(OperationLogService.class); }
        @Bean(destroyMethod = "shutdownNow")
        ExecutorService articleProjectionExecutor() { return Executors.newSingleThreadExecutor(); }
        @Bean(destroyMethod = "shutdownNow")
        ExecutorService operationAuditExecutor() { return Executors.newSingleThreadExecutor(); }
        @Bean PlatformTransactionManager transactions() {
            return new AbstractPlatformTransactionManager() {
                protected Object doGetTransaction() { return new Object(); }
                protected void doBegin(Object t, TransactionDefinition d) {}
                protected void doCommit(DefaultTransactionStatus s) {}
                protected void doRollback(DefaultTransactionStatus s) {}
            };
        }
    }

    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CacheClient cache;
    @Autowired RabbitTemplate rabbit;
    @Autowired LogAspect audit;
    @Autowired OperationLogService operationLogs;
    @Autowired ArticlePublicationListener listener;
    @Autowired ArticleRepository articles;
    @Autowired FeedService feeds;
    @Autowired CacheManager caches;
    @Autowired StringRedisTemplate strings;
    @Autowired RedissonClient redisson;
    @Autowired @Qualifier("articleProjectionExecutor") ExecutorService projections;
    @Autowired @Qualifier("operationAuditExecutor") ExecutorService audits;

    @BeforeEach void resetExternalServices() throws Exception {
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        audits.submit(() -> {}).get(2, TimeUnit.SECONDS);
        reset(cache, rabbit, operationLogs, articles, feeds, redisson);
    }

    @Test
    void committedSaveReturnsWhileCacheProjectionIsBlocked() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> { entered.countDown(); release.await(5, TimeUnit.SECONDS); return null; })
                .when(cache).evictArticle(7L);
        ExecutorService request = Executors.newSingleThreadExecutor();
        try {
            Future<?> saved = request.submit(() -> new TransactionTemplate(transactions).execute(status -> {
                events.publishEvent(new ArticlePublicationChanged(
                        Article.builder().id(7L).published(false).build(), false, false));
                return null;
            }));
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThatCode(() -> saved.get(1, TimeUnit.SECONDS)).doesNotThrowAnyException();
        } finally {
            release.countDown();
            request.shutdownNow();
        }
    }

    @Test
    void successfulSaveReturnsWhileRabbitAuditSendIsBlocked() throws Throwable {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> { entered.countDown(); release.await(5, TimeUnit.SECONDS); return null; })
                .when(rabbit).convertAndSend(eq(RabbitMQConfig.LOG_EXCHANGE), eq(RabbitMQConfig.LOG_ROUTING_KEY), any(Object.class));
        ProceedingJoinPoint point = savePoint();
        Log annotation = SaveEndpoint.class.getMethod("save").getAnnotation(Log.class);
        ExecutorService request = Executors.newSingleThreadExecutor();
        try {
            Future<Object> saved = request.submit(() -> {
                try { return audit.around(point, annotation); }
                catch (Throwable error) { throw new RuntimeException(error); }
            });
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(saved.get(1, TimeUnit.SECONDS)).isEqualTo("saved-7");
        } finally {
            release.countDown();
            request.shutdownNow();
        }
    }

    @Test
    void queuedPublicationUsesItsImmutableTagsAndPublicationTime() throws Exception {
        LocalDateTime publishedAt = LocalDateTime.of(2026, 10, 26, 12, 30);
        Article article = Article.builder().id(7L).published(true).publishedAt(publishedAt)
                .updatedAt(publishedAt).tags(new ArrayList<>(List.of(Tag.builder().id(11L).build()))).build();
        ArticlePublicationChanged change = new ArticlePublicationChanged(article, false, false);
        article.setPublished(false);
        article.setPublishedAt(publishedAt.plusDays(1));
        article.getTags().get(0).setId(99L);
        article.getTags().clear();
        when(articles.existsByIdAndPublishedTrue(7L)).thenReturn(true);
        when(articles.findPublishedTagIds(7L)).thenReturn(List.of(11L));
        listener.afterCommit(change);
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verify(feeds).pushToFollowers(7L, publishedAt, List.of(11L));
    }

    @Test
    void delayedFirstPublicationUsesCurrentTagsAfterAnInterveningEdit() throws Exception {
        LocalDateTime oldVersion = LocalDateTime.of(2026, 10, 26, 12, 30);
        when(articles.existsByIdAndPublishedTrue(7L)).thenReturn(true);
        when(articles.findPublishedTagIds(7L)).thenReturn(List.of(22L));
        listener.afterCommit(new ArticlePublicationChanged(
                Article.builder().id(7L).published(true).publishedAt(oldVersion).updatedAt(oldVersion)
                        .tags(List.of(Tag.builder().id(11L).build())).build(), false, false));
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verify(cache).evictArticle(7L);
        verify(feeds).pushToFollowers(7L, oldVersion, List.of(22L));
    }

    @Test
    void persistedMicrosecondRoundingDoesNotDiscardACommittedPublication() throws Exception {
        LocalDateTime captured = LocalDateTime.of(2026, 10, 26, 12, 30, 0, 123456789);
        when(articles.existsByIdAndPublishedTrue(7L)).thenReturn(true);
        listener.afterCommit(new ArticlePublicationChanged(
                Article.builder().id(7L).published(true).publishedAt(captured).updatedAt(captured).build(), false, false));
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verify(feeds).pushToFollowers(7L, captured, List.of());
    }

    @Test
    void withdrawnPublicationCannotBePushedByItsQueuedPublishEvent() throws Exception {
        when(articles.existsByIdAndPublishedTrue(7L)).thenReturn(false);
        listener.afterCommit(new ArticlePublicationChanged(
                Article.builder().id(7L).published(true).build(), false, false));
        projections.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verifyNoInteractions(feeds, redisson);
    }

    @Test
    void fullProjectionQueueDoesNotRunCacheWorkOnTheSavingThread() throws Exception {
        ThreadPoolExecutor full = new ArticleSideEffectConfig().articleProjectionExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        try {
            full.execute(() -> {
                entered.countDown();
                try { release.await(5, TimeUnit.SECONDS); }
                catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            });
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            int remaining = full.getQueue().remainingCapacity();
            for (int i = 0; i < remaining; i++) full.execute(() -> {});
            ArticlePublicationListener saturated = new ArticlePublicationListener(
                    cache, caches, strings, redisson, feeds, articles, full);
            assertThatCode(() -> saturated.afterCommit(new ArticlePublicationChanged(
                    Article.builder().id(7L).published(false).build(), false, false)))
                    .doesNotThrowAnyException();
            verifyNoInteractions(cache, articles, feeds);
        } finally {
            release.countDown();
            full.shutdownNow();
        }
    }

    @Test
    void rabbitFailureActuallyInvokesTheBackgroundDatabaseFallback() throws Throwable {
        doThrow(new org.springframework.amqp.AmqpConnectException(new java.net.ConnectException("unavailable")))
                .when(rabbit).convertAndSend(eq(RabbitMQConfig.LOG_EXCHANGE), eq(RabbitMQConfig.LOG_ROUTING_KEY), any(Object.class));
        assertThat(audit.around(savePoint(), SaveEndpoint.class.getMethod("save").getAnnotation(Log.class)))
                .isEqualTo("saved-7");
        audits.submit(() -> {}).get(2, TimeUnit.SECONDS);
        verify(operationLogs).saveLog(argThat(log -> log.getStatus() == 1 && "CREATE".equals(log.getOperationType())));
    }

    @Test
    void auditQueueRejectionPreservesTheSuccessfulBusinessResponse() throws Throwable {
        Executor rejected = task -> { throw new RejectedExecutionException("full"); };
        LogAspect saturated = new LogAspect(operationLogs, new MQProducerService(rabbit), rejected);
        assertThat(saturated.around(savePoint(), SaveEndpoint.class.getMethod("save").getAnnotation(Log.class)))
                .isEqualTo("saved-7");
        verifyNoInteractions(rabbit, operationLogs);
    }

    private ProceedingJoinPoint savePoint() throws Throwable {
        ProceedingJoinPoint point = mock(ProceedingJoinPoint.class);
        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getName()).thenReturn("save");
        when(point.getSignature()).thenReturn(signature);
        when(point.getTarget()).thenReturn(new SaveEndpoint());
        when(point.getArgs()).thenReturn(new Object[0]);
        when(point.proceed()).thenReturn("saved-7");
        return point;
    }

    static class SaveEndpoint {
        @Log(module = "articles", operationType = "CREATE", description = "save")
        public String save() { return "saved-7"; }
    }
}
