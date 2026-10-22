-- Apply once on production MySQL before starting this version (additive migration).
-- Keep batch IDs while any Redis pending batch can still be replayed.
-- No article FK: the idempotency tombstone intentionally survives article deletion.
CREATE TABLE IF NOT EXISTS applied_view_count_batches (
 batch_id VARCHAR(36) NOT NULL,
 article_id BIGINT NOT NULL,
 delta BIGINT NOT NULL,
 applied_at DATETIME(6) NOT NULL,
 PRIMARY KEY(batch_id),
 INDEX idx_view_batch_article(article_id),
 INDEX idx_view_batch_applied(applied_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
