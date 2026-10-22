package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myblog.common.redis.CacheClient;
import com.myblog.dto.*;
import com.myblog.entity.*;
import com.myblog.repository.*;
import com.myblog.service.admin.AdminArticleService;

import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.*;
import org.springframework.data.redis.core.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@DataJpaTest(
        properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@Import({
    ArticleService.class,
    AdminArticleService.class,
    ArticlePublicationListener.class,
    ArticlePublicationIntegrationTest.Caches.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ArticlePublicationIntegrationTest {
    @TestConfiguration
    @EnableCaching
    static class Caches {
        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager();
        }
    }

    @Autowired ArticleRepository articles;
    @Autowired UserRepository users;
    @Autowired ArticleService service;
    @Autowired AdminArticleService admin;
    @Autowired CacheManager caches;
    @Autowired PlatformTransactionManager transactions;
    @MockBean CacheClient cache;
    @MockBean RedisTemplate<String, Object> redis;
    @MockBean StringRedisTemplate strings;
    @MockBean RedissonClient redisson;
    @MockBean FeedService feeds;
    User author;

    @BeforeEach
    void setup() {
        articles.deleteAll();
        users.deleteAll();
        for (String name : caches.getCacheNames()) caches.getCache(name).clear();
        author =
                users.saveAndFlush(
                        User.builder()
                                .username("publication-test")
                                .email("publication@example.test")
                                .password("not-a-credential")
                                .role(User.Role.ADMIN)
                                .build());
    }

    private Long draft() {
        ArticleRequest request = new ArticleRequest();
        request.setTitle("private draft");
        request.setContent("draft body");
        request.setPublished(false);
        request.setFeatured(true);
        return service.createArticle(request, author).getId();
    }

    @Test
    void realDraftCanBeEditedButCannotBeReadPublicly() {
        Long id = draft();
        assertThat(service.getAdminArticle(id).getContent()).isEqualTo("draft body");
        assertThatThrownBy(() -> service.getArticle(id))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void withdrawBlocksStaleBodyAndEveryCachedPublicIndex() {
        Long id = draft();
        admin.togglePublish(id, true);
        assertThat(articles.findById(id).orElseThrow().getPublishedAt()).isNotNull();
        new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            service.getFeaturedArticles();
                            service.getArchive();
                            return null;
                        });
        assertThat(caches.getCache("featuredArticleSummaries").get("all")).isNotNull();
        admin.togglePublish(id, false);
        assertThat(caches.getCache("featuredArticleSummaries").get("all")).isNull();
        assertThat(caches.getCache("articleArchiveSummary").get("all")).isNull();
        clearInvocations(cache);
        assertThatThrownBy(() -> service.getArticle(id))
                .isInstanceOf(ResponseStatusException.class);
        verify(cache, never())
                .queryWithLogicalExpire(anyString(), any(), any(), any(), anyLong(), any());
        // A late reader or temporarily failed eviction may put an old index back.
        var summary = ArticleSummaryResponse.builder().id(id).title("old published title").build();
        caches.getCache("featuredArticleSummaries").put("all", List.of(summary));
        caches.getCache("popularArticleSummaries").put(5, List.of(summary));
        var brief = ArchiveResponse.ArticleBrief.builder().id(id).title("old title").build();
        var month = new ArchiveResponse.MonthArchive(10, "October", List.of(brief));
        caches.getCache("articleArchiveSummary")
                .put(
                        "all",
                        new ArchiveResponse(
                                1,
                                List.of(new ArchiveResponse.YearArchive(2026, 1, List.of(month)))));
        assertThat(service.getFeaturedArticles()).isEmpty();
        assertThat(service.getPopularArticles(5)).isEmpty();
        assertThat(service.getArchive().getTotalCount()).isZero();
        assertThat(service.getAdminArticle(id).getContent()).isEqualTo("draft body");
    }

    @Test
    void publishAfterWithdrawalInvalidatesOldBodyAndKeepsNewContent() {
        Long id = draft();
        admin.togglePublish(id, true);
        admin.togglePublish(id, false);
        var edit = new ArticleRequest();
        edit.setTitle("new edition");
        edit.setContent("new edition body");
        edit.setFeatured(true);
        edit.setPublished(false);
        service.updateArticle(id, edit, author);
        clearInvocations(cache);
        doThrow(
                        new org.springframework.data.redis.RedisConnectionFailureException(
                                "simulated invalidation outage"))
                .when(cache)
                .evictArticle(id);
        admin.togglePublish(id, true);
        verify(cache).evictArticle(id);
        when(cache.queryWithLogicalExpire(
                        anyString(), eq(id), eq(ArticleResponse.class), any(), anyLong(), any()))
                .thenReturn(
                        ArticleResponse.builder()
                                .id(id)
                                .published(true)
                                .content("old cached edition")
                                .updatedAt(java.time.LocalDateTime.of(2020, 1, 1, 0, 0))
                                .build());
        assertThat(service.getArticle(id).getContent()).isEqualTo("new edition body");
        var staleSummary =
                ArticleSummaryResponse.builder()
                        .id(id)
                        .title("previous title")
                        .updatedAt(java.time.LocalDateTime.of(2020, 1, 1, 0, 0))
                        .build();
        caches.getCache("featuredArticleSummaries").put("all", List.of(staleSummary));
        assertThat(service.getFeaturedArticles())
                .extracting(ArticleSummaryResponse::getTitle)
                .containsExactly("new edition");
        assertThat(
                        service.getArchive()
                                .getYears()
                                .get(0)
                                .getMonths()
                                .get(0)
                                .getArticles()
                                .get(0)
                                .getTitle())
                .isEqualTo("new edition");
    }

    @Test
    void rolledBackWithdrawalLeavesDatabaseAndCachesPublished() {
        Long id = draft();
        admin.togglePublish(id, true);
        caches.getCache("featuredArticleSummaries").put("all", List.of());
        clearInvocations(cache);
        new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            admin.togglePublish(id, false);
                            status.setRollbackOnly();
                            return null;
                        });
        assertThat(articles.existsByIdAndPublishedTrue(id)).isTrue();
        verify(cache, never()).evictArticle(id);
        assertThat(caches.getCache("featuredArticleSummaries").get("all")).isNotNull();
    }
}
