package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;

import lombok.RequiredArgsConstructor;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ViewCountBuffer {
    public static final String PENDING_PREFIX = "article:view:pending:";
    private final StringRedisTemplate redis;
    private static final DefaultRedisScript<List> CAPTURE = new DefaultRedisScript<>();
    private static final DefaultRedisScript<Long> ACK = new DefaultRedisScript<>();

    static {
        CAPTURE.setLocation(new ClassPathResource("scripts/view_count_capture.lua"));
        CAPTURE.setResultType(List.class);
        ACK.setLocation(new ClassPathResource("scripts/view_count_ack.lua"));
        ACK.setResultType(Long.class);
    }

    public Optional<ViewCountBatch> capture(Long articleId) {
        List<?> result =
                redis.execute(
                        CAPTURE,
                        List.of(
                                RedisKeyPrefix.ARTICLE_VIEW_COUNT + articleId,
                                PENDING_PREFIX + articleId),
                        UUID.randomUUID().toString());
        if (result == null)
            throw new IllegalStateException("View-count capture returned no acknowledgement");
        if (result.isEmpty()) return Optional.empty();
        if (result.size() != 2 || result.get(0) == null || result.get(1) == null)
            throw new IllegalStateException("Invalid pending view-count batch");
        return Optional.of(
                new ViewCountBatch(
                        result.get(0).toString(),
                        articleId,
                        Long.parseLong(result.get(1).toString())));
    }

    public void acknowledge(ViewCountBatch batch) {
        Long result =
                redis.execute(ACK, List.of(PENDING_PREFIX + batch.articleId()), batch.batchId());
        if (result == null)
            throw new IllegalStateException("View-count acknowledgement unavailable");
    }

    public Set<Long> articleIds() {
        Set<Long> ids = new LinkedHashSet<>();
        collect(RedisKeyPrefix.ARTICLE_VIEW_COUNT, ids);
        collect(PENDING_PREFIX, ids);
        return ids;
    }

    private void collect(String prefix, Set<Long> ids) {
        try (Cursor<String> cursor =
                redis.scan(ScanOptions.scanOptions().match(prefix + "*").count(100).build())) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                try {
                    long id = Long.parseLong(key.substring(prefix.length()));
                    if (id > 0) ids.add(id);
                } catch (NumberFormatException ignored) {
                    /* Foreign keys are not valid article counters. */
                }
            }
        }
    }
}
