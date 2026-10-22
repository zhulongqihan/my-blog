package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myblog.common.redis.CacheClient;
import com.myblog.dto.ArticlePublicationVersion;
import com.myblog.dto.ArticleResponse;
import com.myblog.entity.Article;
import com.myblog.repository.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RedissonClient;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ArticleVisibilityTest {
    @Mock ArticleRepository articles;
    @Mock CategoryRepository categories;
    @Mock TagRepository tags;
    @Mock CommentRepository comments;
    @Mock RedisTemplate<String, Object> redis;
    @Mock StringRedisTemplate strings;
    @Mock CacheClient cache;
    @Mock RedissonClient redisson;
    @Mock ApplicationEventPublisher events;
    @InjectMocks ArticleService service;

    @Test
    void withdrawnArticleNeverReadsEvenAnExistingStaleCache() {
        when(articles.findPublishedVersion(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getArticle(7L))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode.value")
                .isEqualTo(404);
        verifyNoInteractions(cache);
    }

    @Test
    void adminReadsDraftDirectlyWithoutPublicBloomOrCache() {
        when(articles.findById(7L))
                .thenReturn(
                        Optional.of(
                                Article.builder()
                                        .id(7L)
                                        .published(false)
                                        .content("draft")
                                        .build()));
        assertThat(service.getAdminArticle(7L).getContent()).isEqualTo("draft");
        verifyNoInteractions(cache);
    }

    @Test
    void publishedArticleUsesItsExistingCache() {
        var updatedAt = java.time.LocalDateTime.of(2026, 10, 22, 12, 0);
        when(articles.findPublishedVersion(7L))
                .thenReturn(Optional.of(new ArticlePublicationVersion(7L, updatedAt)));
        when(cache.queryWithLogicalExpire(
                        anyString(), eq(7L), eq(ArticleResponse.class), any(), anyLong(), any()))
                .thenReturn(
                        ArticleResponse.builder()
                                .id(7L)
                                .published(true)
                                .updatedAt(updatedAt)
                                .content("public")
                                .build());
        assertThat(service.getArticle(7L).getContent()).isEqualTo("public");
    }

    @Test
    void unavailableArticleDoesNotProduceViewCounters() {
        when(articles.findPublishedVersion(7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getArticleAndIncrementView(7L))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(redis);
        verifyNoInteractions(strings);
    }
}
