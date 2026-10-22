package com.myblog.dto;

import java.time.LocalDateTime;

/** Minimal authoritative visibility/version check; never loads a draft body. */
public record ArticlePublicationVersion(Long id, LocalDateTime updatedAt) {}
