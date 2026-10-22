package com.myblog.service;

import static org.assertj.core.api.Assertions.*;

import com.myblog.entity.Article;
import com.myblog.repository.*;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.*;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.concurrent.*;

@DataJpaTest(
        properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.show-sql=false"})
@Import(ViewCountBatchApplier.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ViewCountDatabaseTest {
    @Autowired ArticleRepository articles;
    @Autowired AppliedViewCountBatchRepository batches;
    @Autowired ViewCountBatchApplier applier;
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void clean() {
        batches.deleteAll();
        articles.deleteAll();
    }

    private Article article() {
        return articles.saveAndFlush(
                Article.builder()
                        .title("counter test")
                        .content("old")
                        .viewCount(10)
                        .published(true)
                        .build());
    }

    @Test
    void deletedArticleBatchIsRecordedOnceWithoutResurrectingArticle() {
        var a = article();
        var batch = new ViewCountBatch(UUID.randomUUID().toString(), a.getId(), 3);
        articles.deleteById(a.getId());
        assertThat(applier.apply(batch)).isTrue();
        assertThat(applier.apply(batch)).isFalse();
        assertThat(articles.findById(a.getId())).isEmpty();
        assertThat(batches.count()).isEqualTo(1);
    }

    @Test
    void sameBatchFromConcurrentWorkersIsAppliedExactlyOnce() throws Exception {
        var a = article();
        var batch = new ViewCountBatch(UUID.randomUUID().toString(), a.getId(), 3);
        ExecutorService pool = Executors.newFixedThreadPool(6);
        try {
            List<Future<Boolean>> jobs = new ArrayList<>();
            for (int i = 0; i < 6; i++) jobs.add(pool.submit(() -> applier.apply(batch)));
            int applied = 0;
            for (var result : jobs) if (result.get(20, TimeUnit.SECONDS)) applied++;
            assertThat(applied).isEqualTo(1);
            assertThat(articles.findById(a.getId()).orElseThrow().getViewCount()).isEqualTo(13);
            assertThat(batches.count()).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void staleEditorDoesNotOverwriteACommittedCounterIncrement() throws Exception {
        var a = article();
        var loaded = new CountDownLatch(1);
        var updated = new CountDownLatch(1);
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<?> editor =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactions)
                                            .execute(
                                                    status -> {
                                                        Article editing =
                                                                articles.findById(a.getId())
                                                                        .orElseThrow();
                                                        loaded.countDown();
                                                        try {
                                                            if (!updated.await(
                                                                    15, TimeUnit.SECONDS))
                                                                throw new IllegalStateException(
                                                                        "counter did not complete");
                                                        } catch (InterruptedException e) {
                                                            Thread.currentThread().interrupt();
                                                            throw new IllegalStateException(e);
                                                        }
                                                        editing.setContent("new");
                                                        articles.saveAndFlush(editing);
                                                        return null;
                                                    }));
            assertThat(loaded.await(10, TimeUnit.SECONDS)).isTrue();
            applier.apply(new ViewCountBatch(UUID.randomUUID().toString(), a.getId(), 3));
            updated.countDown();
            editor.get(20, TimeUnit.SECONDS);
            var saved = articles.findById(a.getId()).orElseThrow();
            assertThat(saved.getViewCount()).isEqualTo(13);
            assertThat(saved.getContent()).isEqualTo("new");
        } finally {
            updated.countDown();
            pool.shutdownNow();
        }
    }
}
