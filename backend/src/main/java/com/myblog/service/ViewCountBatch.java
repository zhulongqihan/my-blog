package com.myblog.service;

public record ViewCountBatch(String batchId, Long articleId, long delta) {
    public ViewCountBatch {
        if (batchId == null
                || batchId.isBlank()
                || articleId == null
                || articleId <= 0
                || delta <= 0
                || delta > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Invalid view-count batch");
    }
}
