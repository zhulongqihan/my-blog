package com.myblog.entity;

import jakarta.persistence.*;

import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "applied_view_count_batches",
        indexes = {
            @Index(name = "idx_view_batch_article", columnList = "article_id"),
            @Index(name = "idx_view_batch_applied", columnList = "applied_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppliedViewCountBatch {
    @Id
    @Column(name = "batch_id", length = 36, nullable = false)
    private String batchId;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(nullable = false)
    private long delta;

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt;

    public AppliedViewCountBatch(String batchId, Long articleId, long delta) {
        this.batchId = batchId;
        this.articleId = articleId;
        this.delta = delta;
        this.appliedAt = LocalDateTime.now();
    }
}
