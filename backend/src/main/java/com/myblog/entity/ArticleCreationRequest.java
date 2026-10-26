package com.myblog.entity;

import jakarta.persistence.*;
import lombok.*;

/** Durable creation receipt. Deliberately retained after article deletion. */
@Entity
@Table(name="article_creation_requests", uniqueConstraints=@UniqueConstraint(
        name="uk_article_creation_author_key", columnNames={"author_id", "request_key"}))
@Getter @NoArgsConstructor @AllArgsConstructor
public class ArticleCreationRequest {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;
    @Column(name="author_id",nullable=false) private Long authorId;
    @Column(name="request_key",nullable=false,length=36) private String requestKey;
    @Column(name="request_hash",nullable=false,length=64) private String requestHash;
    @Column(name="article_id",nullable=false) private Long articleId;
}
