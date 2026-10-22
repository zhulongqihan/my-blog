package com.myblog.common.redis;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.myblog.common.constant.RedisKeyPrefix;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * 缓存工具类 — 封装穿透/击穿/雪崩三种防御策略
 *
 * 对标黑马点评 Shop 缓存方案：
 * - queryWithBloomFilter：布隆过滤器 + 空值缓存（防穿透）
 * - queryWithMutex：SETNX 互斥锁重建（防击穿 — 强一致性）
 * - queryWithLogicalExpire：逻辑过期 + 异步重建（防击穿 — 高可用）
 * - TTL 随机偏移（防雪崩）
 */
@Slf4j
@Component
public class CacheClient {
    private static final ObjectMapper CACHE_JSON = new ObjectMapper()
            .registerModule(new JavaTimeModule()).disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then return redis.call('DEL', KEYS[1]) else return 0 end", Long.class);
    private final ThreadPoolExecutor rebuildExecutor = new ThreadPoolExecutor(
            2, 5, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), runnable -> {
                Thread thread = new Thread(runnable, "article-cache-rebuild");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.AbortPolicy());

    public CacheClient(@Qualifier("cacheStringRedisTemplate") StringRedisTemplate stringRedisTemplate,
                       RedissonClient redissonClient) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redissonClient = redissonClient;
    }

    // ========== 写入方法 ==========

    /**
     * 写入缓存（带随机偏移 TTL 防雪崩）
     */
    public void set(String key, Object value, Long time, TimeUnit unit) {
        long randomOffset = RandomUtil.randomLong(-300, 301); // ±5分钟
        long ttlSeconds = unit.toSeconds(time) + randomOffset;
        if (ttlSeconds <= 0) ttlSeconds = unit.toSeconds(time);
        stringRedisTemplate.opsForValue().set(key, encode(value), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 写入逻辑过期缓存（无物理 TTL）
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, encode(redisData));
    }

    // ========== 查询方法 ==========

    /**
     * 布隆过滤器 + 空值缓存（防穿透）+ 随机 TTL（防雪崩）
     */
    public <R, ID> R queryWithBloomFilter(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit
    ) {
        String key = keyPrefix + id;

        // ① 布隆过滤器前置拦截
        RBloomFilter<Long> bloomFilter = redissonClient.getBloomFilter(RedisKeyPrefix.BLOOM_ARTICLE_ID);
        if (bloomFilter.isExists() && !bloomFilter.contains(Long.valueOf(id.toString()))) {
            log.debug("[CacheClient] 布隆过滤器拦截: id={}", id);
            return null;
        }

        // ② 查 Redis 缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return decode(json, type);
        }
        // 命中空值缓存（防穿透兜底）
        if (json != null) {
            return null;
        }

        // ③ 缓存未命中 → 互斥锁重建
        return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
    }

    /**
     * SETNX 互斥锁重建（防击穿 — 强一致性）
     */
    public <R, ID> R queryWithMutex(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit
    ) {
        try {
            return queryWithMutexAvailable(keyPrefix, id, type, dbFallback, time, unit);
        } catch (org.springframework.dao.DataAccessException exception) {
            log.warn("[CacheClient] 缓存不可用，使用数据库读取");
            return dbFallback.apply(id);
        }
    }

    private <R, ID> R queryWithMutexAvailable(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit
    ) {
        String key = keyPrefix + id;

        String lockKey = RedisKeyPrefix.LOCK_CACHE_REBUILD + key;
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
        while (true) {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) return StrUtil.isBlank(json) ? null : decode(json, type);
            String token = tryLock(lockKey);
            if (token != null) {
                try {
                    json = stringRedisTemplate.opsForValue().get(key);
                    if (json != null) return StrUtil.isBlank(json) ? null : decode(json, type);
                    R data = dbFallback.apply(id);
                    if (data == null) {
                        stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
                    } else {
                        set(key, data, time, unit);
                    }
                    return data;
                } finally {
                    unlock(lockKey, token);
                }
            }
            if (System.nanoTime() >= deadline) {
                throw new com.myblog.common.exception.BusinessException("内容正在更新，请稍后重试");
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("缓存等待已中断", e);
            }
        }
    }

    /**
     * 逻辑过期方案（防击穿 — 高可用，用于精选/热门文章）
     */
    public <R, ID> R queryWithLogicalExpire(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit
    ) {
        try {
            return queryWithLogicalExpireAvailable(keyPrefix, id, type, dbFallback, time, unit);
        } catch (org.springframework.dao.DataAccessException exception) {
            log.warn("[CacheClient] 缓存不可用，使用数据库读取");
            return dbFallback.apply(id);
        }
    }

    private <R, ID> R queryWithLogicalExpireAvailable(
            String keyPrefix, ID id, Class<R> type,
            Function<ID, R> dbFallback, Long time, TimeUnit unit
    ) {
        String key = keyPrefix + id;

        // ① 查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isBlank(json)) {
            // 逻辑过期方案前提：热门数据已预热，查不到说明非热门
            return null;
        }

        // ② 反序列化，判断逻辑过期
        RedisData redisData = decode(json, RedisData.class);
        R data;
        try { data = CACHE_JSON.convertValue(redisData.getData(), type); }
        catch (IllegalArgumentException exception) { data = JSONUtil.toBean(JSONUtil.parseObj(redisData.getData()), type); }
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，直接返回
            return data;
        }

        // ③ 已逻辑过期 → 尝试获取锁，异步重建
        String lockKey = RedisKeyPrefix.LOCK_CACHE_REBUILD + key;
        String token = tryLock(lockKey);
        if (token != null) {
            try {
            rebuildExecutor.execute(() -> {
                try {
                    R freshData = dbFallback.apply(id);
                    if (freshData != null) {
                        setWithLogicalExpire(key, freshData, time, unit);
                    } else {
                        stringRedisTemplate.delete(key);
                    }
                } catch (Exception e) {
                    log.error("[CacheClient] 异步重建缓存失败: key={}", key, e);
                } finally {
                    unlock(lockKey, token);
                }
            });
            } catch (RejectedExecutionException e) {
                unlock(lockKey, token);
                log.warn("[CacheClient] 重建队列已满，保留旧缓存");
            }
        }

        // ④ 无论是否获取锁，先返回旧数据（保证高可用）
        return data;
    }

    // ========== 锁操作 ==========

    String tryLock(String key) {
        String token = UUID.randomUUID().toString();
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, token, 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag) ? token : null;
    }

    void unlock(String key, String token) {
        if (token != null) stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), token);
    }

    public void evictArticle(Long id) {
        stringRedisTemplate.delete(List.of(RedisKeyPrefix.ARTICLE_DETAIL + id,
                RedisKeyPrefix.ARTICLE_DETAIL_LOGIC + id));
    }

    private String encode(Object value) {
        try { return CACHE_JSON.writeValueAsString(value); }
        catch (java.io.IOException exception) { throw new IllegalArgumentException("Cannot serialize article cache", exception); }
    }

    private <R> R decode(String json, Class<R> type) {
        try { return CACHE_JSON.readValue(json, type); }
        catch (java.io.IOException exception) {
            // Read old Hutool numeric-date entries; publication versions reject stale precision and rebuild them.
            return JSONUtil.toBean(json, type);
        }
    }

    @PreDestroy
    public void close() {
        rebuildExecutor.shutdownNow();
    }
}
