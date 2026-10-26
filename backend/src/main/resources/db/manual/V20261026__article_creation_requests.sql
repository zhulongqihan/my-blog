-- Apply explicitly before deploying keyed article creation with ddl-auto=validate.
-- Additive: application rollback retains this table and its receipts. Never truncate it.
CREATE TABLE IF NOT EXISTS article_creation_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    request_key VARCHAR(36) NOT NULL,
    request_hash VARCHAR(64) NOT NULL,
    article_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_article_creation_author_key UNIQUE (author_id, request_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
