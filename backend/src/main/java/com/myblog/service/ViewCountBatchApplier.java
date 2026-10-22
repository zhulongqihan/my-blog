package com.myblog.service;

import com.myblog.entity.AppliedViewCountBatch;
import com.myblog.repository.*;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ViewCountBatchApplier {
    private final ArticleRepository articles;
    private final AppliedViewCountBatchRepository batches;

    /**
     * The row lock serializes same-article batches. The unique batch record and counter update
     * commit together, so a Redis acknowledgement can safely be retried.
     */
    @Transactional
    public boolean apply(ViewCountBatch batch) {
        var article = articles.findByIdForUpdate(batch.articleId());
        var applied = batches.findById(batch.batchId());
        if (applied.isPresent()) {
            if (!applied.get().getArticleId().equals(batch.articleId())
                    || applied.get().getDelta() != batch.delta())
                throw new IllegalStateException("View-count batch identity mismatch");
            return false;
        }
        if (article.isPresent()) {
            Math.toIntExact(
                    (long) (article.get().getViewCount() == null ? 0 : article.get().getViewCount())
                            + batch.delta());
            articles.incrementViewCount(batch.articleId(), Math.toIntExact(batch.delta()));
        }
        // A deleted article is recorded as consumed too; its removed counter must not resurrect
        // content.
        batches.saveAndFlush(
                new AppliedViewCountBatch(batch.batchId(), batch.articleId(), batch.delta()));
        return true;
    }
}
