package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.dto.*;
import com.myblog.entity.*;
import com.myblog.repository.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.*;
import org.springframework.transaction.annotation.*;

@DataJpaTest(properties={"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.generate_statistics=true"})
@Import({ArticleService.class, com.myblog.service.admin.AdminArticleService.class})
@Transactional(propagation=Propagation.NOT_SUPPORTED)
class ArticleCreationIntegrationTest {
    @Autowired ArticleService service;
    @Autowired com.myblog.service.admin.AdminArticleService admin;
    @Autowired jakarta.persistence.EntityManagerFactory entityManagers;
    @Autowired ArticleRepository articles;
    @Autowired UserRepository users;
    @Autowired ArticleCreationRequestRepository receipts;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    @MockBean com.myblog.common.redis.CacheClient cache;
    @MockBean RedisTemplate<String,Object> redis;
    @MockBean StringRedisTemplate strings;
    @MockBean RedissonClient redisson;
    @MockBean FeedService feeds;
    User author;

    @BeforeEach void setup() {
        receipts.deleteAll(); articles.deleteAll(); users.deleteAll();
        author=users.saveAndFlush(User.builder().username("creation-test").email("creation@example.test")
            .password("not-a-credential").role(User.Role.ADMIN).build());
        when(strings.opsForValue()).thenThrow(new AssertionError("admin save must not contact Redis"));
    }

    ArticleRequest request() {
        ArticleRequest r=new ArticleRequest(); r.setTitle("draft");r.setContent("body");r.setPublished(false);return r;
    }

    @Test void saveUpdateAndAdminReadDoNotWaitForRedis() {
        ArticleResponse created=service.createArticle(request(),author);
        assertThat(created.getId()).isNotNull();
        assertThat(service.getAdminArticle(created.getId()).getContent()).isEqualTo("body");
        ArticleRequest edited=request();edited.setContent("edited");
        assertThat(service.updateArticle(created.getId(),edited,author).getContent()).isEqualTo("edited");
        verifyNoInteractions(strings);
    }

    @Test void lostResponseReplayReturnsOriginalIdAndDoesNotOverwriteNewerEdits() {
        String key=UUID.randomUUID().toString();
        Long id=service.createArticle(request(),author,key).getId();
        ArticleRequest edited=request();edited.setContent("newer revision");
        service.updateArticle(id,edited,author);
        assertThat(service.createArticle(request(),author,key).getId()).isEqualTo(id);
        assertThat(service.getAdminArticle(id).getContent()).isEqualTo("newer revision");
        assertThat(articles.count()).isEqualTo(1);assertThat(receipts.count()).isEqualTo(1);
    }

    @Test void sameKeyWithChangedPayloadIsConflictWithoutWriting() {
        String key=UUID.randomUUID().toString();
        service.createArticle(request(),author,key);
        ArticleRequest changed=request();changed.setContent("different");
        assertStatus(409, () -> service.createArticle(changed,author,key));
        assertThat(articles.count()).isEqualTo(1);
    }

    @Test void concurrentRetriesHaveOneArticleAndOneReceipt() throws Exception {
        String key=UUID.randomUUID().toString();
        var pool=java.util.concurrent.Executors.newFixedThreadPool(4);
        var start=new java.util.concurrent.CountDownLatch(1);
        try {
            List<java.util.concurrent.Future<Long>> writes=new ArrayList<>();
            for(int i=0;i<4;i++) writes.add(pool.submit(() -> {
                start.await();return service.createArticle(request(),author,key).getId();
            }));
            start.countDown();
            Set<Long> ids=new HashSet<>();
            for(var write:writes) ids.add(write.get(8,java.util.concurrent.TimeUnit.SECONDS));
            assertThat(ids).hasSize(1);assertThat(articles.count()).isEqualTo(1);assertThat(receipts.count()).isEqualTo(1);
        } finally {pool.shutdownNow();}
    }

    @Test void rollbackLeavesNoReceiptAndSameKeyCanRetry() {
        String key=UUID.randomUUID().toString();
        new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status -> {
            service.createArticle(request(),author,key);status.setRollbackOnly();return null;
        });
        assertThat(receipts.count()).isZero();assertThat(articles.count()).isZero();
        assertThat(service.createArticle(request(),author,key).getId()).isNotNull();
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test void receiptSurvivesDeletionAndDoesNotRecreateArticle() {
        String key=UUID.randomUUID().toString();Long id=service.createArticle(request(),author,key).getId();
        service.deleteArticle(id,author);
        assertStatus(410, () -> service.createArticle(request(),author,key));
        assertThat(articles.count()).isZero();assertThat(receipts.count()).isEqualTo(1);
    }

    @Test void independentKeysAndAuthorsDoNotDeduplicateByTitle() {
        String key=UUID.randomUUID().toString();
        service.createArticle(request(),author,key);
        service.createArticle(request(),author,UUID.randomUUID().toString());
        User other=users.saveAndFlush(User.builder().username("another").email("another@example.test")
                .password("not-a-credential").role(User.Role.ADMIN).build());
        service.createArticle(request(),other,key);
        assertThat(articles.count()).isEqualTo(3);assertThat(receipts.count()).isEqualTo(3);
    }

    @Test void malformedKeyIsRejectedBeforeWriting() {
        assertStatus(400, () -> service.createArticle(request(),author,"bad-key"));
        assertThat(articles.count()).isZero();
    }

    @Test void listLoadsRelatedDataAndCountsWithBoundedDatabaseQueries() {
        for(int i=0;i<12;i++) service.createArticle(request(),author);
        var stats=entityManagers.unwrap(org.hibernate.SessionFactory.class).getStatistics();stats.clear();
        var query=new com.myblog.dto.admin.ArticleQueryRequest(); query.setPage(1);query.setSize(10);
        var page=admin.getArticles(query);
        assertThat(page.getRecords()).hasSize(10);
        assertThat(page.getTotal()).isEqualTo(12);
        assertThat(stats.getPrepareStatementCount()).isLessThanOrEqualTo(5);
    }

    private void assertStatus(int status, org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(org.springframework.web.server.ResponseStatusException.class,
            error -> assertThat(error.getStatusCode().value()).isEqualTo(status));
    }
}
