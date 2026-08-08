package com.myblog.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.dto.AiTraceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTraceService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void record(AiTraceResponse trace) {
        try {
            String value = objectMapper.writeValueAsString(trace);
            stringRedisTemplate.opsForList().leftPush(RedisKeyPrefix.AI_TRACE_RECENT, value);
            stringRedisTemplate.opsForList().trim(RedisKeyPrefix.AI_TRACE_RECENT, 0, 199);
            stringRedisTemplate.expire(RedisKeyPrefix.AI_TRACE_RECENT, Duration.ofDays(7));
        } catch (Exception ex) {
            log.warn("记录 AI 请求轨迹失败: {}", ex.getMessage());
        }
    }

    public List<AiTraceResponse> recent(int limit) {
        try {
            List<String> values = stringRedisTemplate.opsForList()
                    .range(RedisKeyPrefix.AI_TRACE_RECENT, 0, Math.max(1, Math.min(limit, 200)) - 1);
            if (values == null) return Collections.emptyList();
            return values.stream().map(this::parse).filter(java.util.Objects::nonNull).toList();
        } catch (Exception ex) {
            log.warn("读取 AI 请求轨迹失败: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private AiTraceResponse parse(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() { });
        } catch (Exception ex) {
            return null;
        }
    }
}
