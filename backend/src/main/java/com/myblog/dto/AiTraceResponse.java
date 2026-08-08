package com.myblog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiTraceResponse {
    private String requestId;
    private String mode;
    private String model;
    private String status;
    private long retrievalCount;
    private long inputChars;
    private long outputChars;
    private long timeToFirstTokenMs;
    private long totalLatencyMs;
    private Instant createdAt;
    private List<AiSource> sources;
    private String error;
}
