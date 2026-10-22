package com.myblog.task;

import com.myblog.service.ViewCountBatchApplier;
import com.myblog.service.ViewCountBuffer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Atomically hand Redis increments to a recoverable pending batch, apply it once in the database,
 * then acknowledge that exact batch. Pending batches survive failures before/after DB commit; new
 * INCRs continue into the active counter.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncTask {
    private final ViewCountBuffer buffer;
    private final ViewCountBatchApplier applier;

    @Scheduled(fixedRate = 5 * 60 * 1000, initialDelay = 60 * 1000)
    public void syncViewCounts() {
        for (Long articleId : buffer.articleIds()) {
            try {
                buffer.capture(articleId)
                        .ifPresent(
                                batch -> {
                                    applier.apply(batch);
                                    buffer.acknowledge(batch);
                                });
            } catch (RuntimeException exception) {
                log.error(
                        "View-count batch retained for retry, articleId={}", articleId, exception);
            }
        }
    }
}
