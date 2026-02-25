package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.result.Result;
import com.myblog.service.CacheService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理端缓存监控控制器
 * 
 * 接口列表：
 *   GET    /api/admin/cache/stats      - 获取缓存统计（Redis信息 + 各空间Key数量）
 *   GET    /api/admin/cache/names      - 获取所有缓存空间名称
 *   DELETE /api/admin/cache/{name}     - 清除指定缓存空间
 *   DELETE /api/admin/cache/all        - 清除所有缓存
 */
@RestController
@RequestMapping("/api/admin/cache")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCacheController {

    private final CacheService cacheService;

    /**
     * 获取缓存综合统计
     * 返回：Redis服务器信息 + 各缓存空间统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("redisInfo", cacheService.getRedisInfo());
        stats.put("cacheSpaces", cacheService.getCacheSpaceStats());
        return Result.success(stats);
    }

    /**
     * 获取所有缓存空间名称
     */
    @GetMapping("/names")
    public Result<Collection<String>> getCacheNames() {
        return Result.success(cacheService.getCacheNames());
    }

    /**
     * 清除指定缓存空间
     */
    @DeleteMapping("/{cacheName}")
    @Log(module = "缓存管理", operationType = "DELETE", description = "清除指定缓存")
    public Result<String> evictCache(@PathVariable String cacheName) {
        cacheService.evictCache(cacheName);
        return Result.success("缓存空间 [" + cacheName + "] 已清除");
    }

    /**
     * 清除所有缓存
     */
    @DeleteMapping("/all")
    @Log(module = "缓存管理", operationType = "DELETE", description = "清除所有缓存")
    public Result<String> evictAllCaches() {
        cacheService.evictAllCaches();
        return Result.success("所有缓存已清除");
    }
}
