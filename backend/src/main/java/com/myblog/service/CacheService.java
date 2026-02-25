package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 缓存管理服务
 * 
 * 功能：
 *   1. 缓存统计（各缓存空间的Key数量、内存使用）
 *   2. 手动清除缓存（按空间或全部）
 *   3. 缓存预热（启动时主动加载热点数据）
 *   4. Redis 信息查询
 * 
 * 面试亮点：
 *   - 缓存可观测性设计
 *   - 利用 Redis INFO 命令获取运行状态
 *   - CacheManager 统一管理 Spring Cache
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CacheManager cacheManager;

    /**
     * 获取 Redis 服务器信息
     */
    public Map<String, Object> getRedisInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        
        try {
            Properties redisInfo = redisTemplate.execute((RedisCallback<Properties>) connection -> 
                    connection.serverCommands().info());
            
            if (redisInfo != null) {
                info.put("redisVersion", redisInfo.getProperty("redis_version", "unknown"));
                info.put("usedMemory", redisInfo.getProperty("used_memory_human", "0B"));
                info.put("usedMemoryPeak", redisInfo.getProperty("used_memory_peak_human", "0B"));
                info.put("connectedClients", redisInfo.getProperty("connected_clients", "0"));
                info.put("uptimeInDays", redisInfo.getProperty("uptime_in_days", "0"));
                info.put("totalKeys", getTotalKeyCount());
                
                // 命中率计算
                long hits = Long.parseLong(redisInfo.getProperty("keyspace_hits", "0"));
                long misses = Long.parseLong(redisInfo.getProperty("keyspace_misses", "0"));
                double hitRate = (hits + misses) > 0 
                        ? (double) hits / (hits + misses) * 100 : 0;
                info.put("keyspaceHits", hits);
                info.put("keyspaceMisses", misses);
                info.put("hitRate", String.format("%.2f", hitRate));
            }
        } catch (Exception e) {
            log.error("获取Redis信息失败", e);
            info.put("error", "无法连接Redis");
        }
        
        return info;
    }

    /**
     * 获取各缓存空间的统计信息
     */
    public List<Map<String, Object>> getCacheSpaceStats() {
        List<Map<String, Object>> stats = new ArrayList<>();
        
        // 定义缓存空间及其描述
        Map<String, String> cacheSpaces = new LinkedHashMap<>();
        cacheSpaces.put("articleDetail", "文章详情");
        cacheSpaces.put("featuredArticles", "精选文章");
        cacheSpaces.put("popularArticles", "热门文章");
        cacheSpaces.put("categories", "分类列表");
        cacheSpaces.put("tags", "标签列表");
        cacheSpaces.put("dashboardStats", "仪表盘统计");
        
        for (Map.Entry<String, String> entry : cacheSpaces.entrySet()) {
            Map<String, Object> spaceStat = new LinkedHashMap<>();
            spaceStat.put("name", entry.getKey());
            spaceStat.put("description", entry.getValue());
            
            // 统计该空间下的Key数量
            String pattern = entry.getKey() + "::*";
            Set<String> keys = redisTemplate.keys(pattern);
            spaceStat.put("keyCount", keys != null ? keys.size() : 0);
            
            stats.add(spaceStat);
        }
        
        // 添加其他Redis Key统计
        addKeyStats(stats, RedisKeyPrefix.ARTICLE_VIEW_COUNT + "*", "viewCountBuffer", "浏览量缓冲区");
        addKeyStats(stats, RedisKeyPrefix.DAILY_VIEW_COUNT + "*", "dailyViews", "每日访问量");
        addKeyStats(stats, RedisKeyPrefix.JWT_BLACKLIST + "*", "jwtBlacklist", "JWT黑名单");
        addKeyStats(stats, RedisKeyPrefix.RATE_LIMIT + "*", "rateLimit", "限流计数器");
        
        return stats;
    }

    /**
     * 清除指定缓存空间
     */
    public void evictCache(String cacheName) {
        org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("[Cache CLEAR] 手动清除缓存空间: {}", cacheName);
        } else {
            throw new RuntimeException("缓存空间不存在: " + cacheName);
        }
    }

    /**
     * 清除所有Spring Cache缓存
     */
    public void evictAllCaches() {
        cacheManager.getCacheNames().forEach(name -> {
            org.springframework.cache.Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        log.info("[Cache CLEAR] 手动清除所有缓存，共 {} 个空间", cacheManager.getCacheNames().size());
    }

    /**
     * 获取缓存空间名称列表
     */
    public Collection<String> getCacheNames() {
        return cacheManager.getCacheNames();
    }

    /**
     * 获取 Redis 总 Key 数
     */
    private long getTotalKeyCount() {
        try {
            Long size = redisTemplate.execute((RedisCallback<Long>) connection -> 
                    connection.serverCommands().dbSize());
            return size != null ? size : 0L;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 辅助方法：添加Key统计
     */
    private void addKeyStats(List<Map<String, Object>> stats, String pattern, String name, String description) {
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("name", name);
        stat.put("description", description);
        Set<String> keys = redisTemplate.keys(pattern);
        stat.put("keyCount", keys != null ? keys.size() : 0);
        stats.add(stat);
    }
}
