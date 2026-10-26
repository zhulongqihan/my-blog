package com.myblog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.dto.ArticleRequest;
import java.security.MessageDigest;
import java.util.*;

final class ArticleCreationFingerprint {
    private ArticleCreationFingerprint() {}
    static String of(ArticleRequest request) {
        // Fixed fields, including nulls, avoid dependence on JSON field order or map iteration.
        List<Object> fields=Arrays.asList(request.getTitle(), request.getSummary(), request.getContent(),
                request.getCoverImage(), request.getCategoryId(), request.getTagIds(),
                request.getPublished(), request.getFeatured());
        try {
            byte[] bytes=new ObjectMapper().writeValueAsBytes(fields);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception impossible) { throw new IllegalStateException("Cannot fingerprint creation request", impossible); }
    }
}
