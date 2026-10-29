package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.entity.*;
import com.myblog.repository.*;
import com.myblog.service.admin.DashboardService;
import jakarta.persistence.EntityManagerFactory;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.*;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false",
        "spring.jpa.open-in-view=false", "spring.jpa.properties.hibernate.generate_statistics=true"})
@Import(DashboardService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DashboardReadIntegrationTest {
    @Autowired DashboardService dashboard;
    @Autowired ArticleRepository articles;
    @Autowired CommentRepository comments;
    @Autowired UserRepository users;
    @Autowired CategoryRepository categories;
    @Autowired EntityManagerFactory entityManagers;
    @Autowired JdbcTemplate jdbc;
    @MockBean RedisTemplate<String, Object> redis;
    @MockBean StringRedisTemplate strings;
    ValueOperations<String, Object> values;
    LocalDate today;
    Long popularId;

    @BeforeEach void setup() {
        jdbc.update("DELETE FROM comments"); jdbc.update("DELETE FROM article_tags");
        jdbc.update("DELETE FROM articles"); jdbc.update("DELETE FROM sys_user_role");
        jdbc.update("DELETE FROM users"); jdbc.update("DELETE FROM categories");
        today = LocalDate.now();
        values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(anyString())).thenReturn(0L);
        when(redis.executePipelined(any(SessionCallback.class))).thenAnswer(invocation -> {
            List<String> keys = new ArrayList<>();
            RedisOperations<String, Object> operations = mock(RedisOperations.class);
            ValueOperations<String, Object> pipelined = mock(ValueOperations.class);
            when(operations.opsForValue()).thenReturn(pipelined);
            when(pipelined.get(anyString())).thenAnswer(read -> {
                keys.add(read.getArgument(0)); return null;
            });
            SessionCallback<?> callback = invocation.getArgument(0);
            assertThat(callback.execute(operations)).isNull();
            assertThat(keys).containsExactlyElementsOf(java.util.stream.IntStream.range(0,7)
                    .mapToObj(i -> RedisKeyPrefix.DAILY_VIEW_COUNT + today.minusDays(6-i)).toList());
            return keys.stream().map(key -> key.endsWith(today.toString()) ? (Object) 9L
                    : key.endsWith(today.minusDays(6).toString()) ? 3L : null).toList();
        });
    }

    private void fixture() {
        User author = users.saveAndFlush(User.builder().username("dashboard-test").email("dashboard@example.test")
                .password("not-a-credential").nickname("registered author").role(User.Role.ADMIN).build());
        Category category = categories.saveAndFlush(Category.builder().name("technical").build());
        categories.saveAndFlush(Category.builder().name("empty").build());
        Article popular = article("published today", true, 11, today.atStartOfDay(), author, category);
        popularId = popular.getId();
        article("private draft", false, 1000, today.atTime(23,59,59,999999000), author, category);
        article("published yesterday", true, 8, today.atStartOfDay().minusNanos(1000), author, null);
        article("first day", true, 3, today.minusDays(6).atStartOfDay(), author, category);
        article("outside trend", false, 4, today.minusDays(6).atStartOfDay().minusNanos(1000), author, null);
        Comment registered = comments.saveAndFlush(Comment.builder().article(popular).user(author)
                .content("registered content").build());
        Comment guest = comments.saveAndFlush(Comment.builder().article(popular).guestName("guest author")
                .content("guest content").approved(false).build());
        jdbc.update("UPDATE comments SET created_at=? WHERE id=?", today.atTime(10,0), registered.getId());
        jdbc.update("UPDATE comments SET created_at=? WHERE id=?", today.atTime(9,0), guest.getId());
    }

    private Article article(String title, boolean published, int views, LocalDateTime created, User author, Category category) {
        return articles.saveAndFlush(Article.builder().title(title).content("large body ".repeat(1000))
                .author(author).category(category).published(published).viewCount(views)
                .createdAt(created).updatedAt(created).build());
    }

    @Test void statisticsKeepDraftAndDateSemanticsWithoutLoadingArticleBodies() {
        fixture();
        var statistics = entityManagers.unwrap(SessionFactory.class).getStatistics(); statistics.clear();
        var result = dashboard.getStats();
        assertThat(result.getTotalArticles()).isEqualTo(5);
        assertThat(result.getTotalViews()).isEqualTo(1026);
        assertThat(result.getTotalUsers()).isEqualTo(1);
        assertThat(result.getTotalComments()).isEqualTo(2);
        assertThat(result.getTodayArticles()).isEqualTo(2);
        assertThat(result.getTodayComments()).isEqualTo(2);
        assertThat(result.getTodayViews()).isEqualTo(9);
        assertThat(result.getViewTrend()).extracting("count").containsExactly(3L,0L,0L,0L,0L,0L,9L);
        assertThat(result.getArticleTrend()).extracting("count").containsExactly(1L,0L,0L,0L,0L,1L,2L);
        assertThat(result.getArticleTrend()).extracting("date").containsExactlyElementsOf(
                java.util.stream.IntStream.range(0,7).mapToObj(i -> today.minusDays(6-i)
                        .format(DateTimeFormatter.ofPattern("MM-dd"))).toList());
        assertThat(result.getPopularArticles()).extracting("title")
                .containsExactly("published today", "published yesterday", "first day");
        assertThat(result.getPopularArticles().get(0).getId()).isEqualTo(popularId);
        assertThat(result.getPopularArticles().get(0).getCommentCount()).isEqualTo(2);
        assertThat(result.getRecentComments()).extracting("authorName")
                .containsExactly("registered author", "guest author");
        assertThat(result.getRecentComments()).extracting("articleTitle")
                .containsOnly("published today");
        assertThat(result.getCategoryStats()).filteredOn(c -> c.getName().equals("technical"))
                .extracting("articleCount").containsExactly(3L);
        assertThat(result.getCategoryStats()).filteredOn(c -> c.getName().equals("empty"))
                .extracting("articleCount").containsExactly(0L);
        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(12);
        assertThat(statistics.getEntityLoadCount()).isZero();
        verify(redis).executePipelined(any(SessionCallback.class));
        verify(values, never()).multiGet(anyCollection());
        verify(values, never()).get(anyString());
    }

    @Test void emptyDatabaseStillReturnsSevenOrderedDaysAndEmptyCollections() {
        var result = dashboard.getStats();
        assertThat(result.getTotalArticles()).isZero();
        assertThat(result.getTotalViews()).isZero();
        assertThat(result.getArticleTrend()).hasSize(7).allSatisfy(day -> assertThat(day.getCount()).isZero());
        assertThat(result.getPopularArticles()).isEmpty();
        assertThat(result.getRecentComments()).isEmpty();
        assertThat(result.getCategoryStats()).isEmpty();
        verify(redis).executePipelined(any(SessionCallback.class));
        verify(values, never()).multiGet(anyCollection());
        verify(values, never()).get(anyString());
    }

    @Test void redisFailuresAndInvalidCountersRemainErrorsRatherThanFakeZeroStatistics() {
        doThrow(new org.springframework.data.redis.RedisConnectionFailureException("isolated unavailable"))
                .when(redis).executePipelined(any(SessionCallback.class));
        assertThatThrownBy(dashboard::getStats)
                .isInstanceOf(org.springframework.data.redis.RedisConnectionFailureException.class);
        doReturn(Arrays.asList(0L,0L,0L,0L,0L,0L,"invalid"))
                .when(redis).executePipelined(any(SessionCallback.class));
        assertThatThrownBy(dashboard::getStats).isInstanceOf(ClassCastException.class);
        doThrow(new org.springframework.data.redis.RedisSystemException("WRONGTYPE", null))
                .when(redis).executePipelined(any(SessionCallback.class));
        assertThatThrownBy(dashboard::getStats).isInstanceOf(org.springframework.data.redis.RedisSystemException.class);
    }

    @Test void oneDateSnapshotIsUsedEvenIfMidnightPassesDuringTheRead() {
        try (var dates = mockStatic(LocalDate.class, CALLS_REAL_METHODS)) {
            dates.when(LocalDate::now).thenReturn(today, today.plusDays(1));
            dates.clearInvocations();
            var result = dashboard.getStats();
            assertThat(result.getViewTrend().get(6).getDate())
                    .isEqualTo(today.format(DateTimeFormatter.ofPattern("MM-dd")));
            assertThat(result.getArticleTrend().get(6).getDate())
                    .isEqualTo(result.getViewTrend().get(6).getDate());
            dates.verify(LocalDate::now, times(1));
        }
    }
}
