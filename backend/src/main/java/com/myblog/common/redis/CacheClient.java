package com.myblog.common.redis;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.myblog.common.constant.RedisKeyPrefix;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR =
            Executors.newFixedThreadPool(5);

    public CacheClient(StringRedisTemplate stringRedisTemplate, RedissonClient redissonClient) {
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
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), ttlSeconds, TimeUnit.SECONDS);
    }

    /**
     * 写入逻辑过期缓存（无物理 TTL）
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit unit) {
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData));
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
            return JSONUtil.toBean(json, type);
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
        String key = keyPrefix + id;

        // ① 查缓存
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StrUtil.isNotBlank(json)) {
            return JSONUtil.toBean(json, type);
        }
        if (json != null) return null; // 空值缓存

        // ② 尝试获取互斥锁
        String lockKey = RedisKeyPrefix.LOCK_CACHE_REBUILD + id;
        boolean isLock = tryLock(lockKey);

        try {
            if (!isLock) {
                // 未获取锁，休眠后重试
                Thread.sleep(50);
                return queryWithMutex(keyPrefix, id, type, dbFallback, time, unit);
            }

            // ③ DoubleCheck：再查一次缓存
            json = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(json)) {
                return JSONUtil.toBean(json, type);
            }

            // ④ 查数据库并重建缓存
            R data = dbFallback.apply(id);
            if (data == null) {
                // 缓存空值，TTL 2 分钟
                stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
                return null;
            }
            // 写入缓存（带随机偏移防雪崩）
            set(key, data, time, unit);
            return data;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            unlock(lockKey);
        }
    }

    /**
     * 逻辑过期方案（防击穿 — 高可用，用于精选/热门文章）
     */
    public <R, ID> R queryWithLogicalExpire(
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
        RedisData redisData = JSONUtil.toBean(json, RedisData.class);
        R data = JSONUtil.toBean((JSONObject) redisData.getData(), type);
        LocalDateTime expireTime = redisData.getExpireTime();

        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，直接返回
            return data;
        }

        // ③ 已逻辑过期 → 尝试获取锁，异步重建
        String lockKey = RedisKeyPrefix.LOCK_CACHE_REBUILD + id;
        boolean isLock = tryLock(lockKey);
        if (isLock) {
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    R freshData = dbFallback.apply(id);
                    if (freshData != null) {
                        setWithLogicalExpire(key, freshData, time, unit);
                    }
                } catch (Exception e) {
                    log.error("[CacheClient] 异步重建缓存失败: key={}", key, e);
                } finally {
                    unlock(lockKey);
                }
            });
        }

        // ④ 无论是否获取锁，先返回旧数据（保证高可用）
        return data;
    }

    // ========== 锁操作 ==========

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }
}
