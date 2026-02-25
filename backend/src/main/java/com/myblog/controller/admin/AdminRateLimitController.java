package com.myblog.controller.admin;

import com.myblog.common.annotation.Log;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.result.Result;
import com.myblog.service.IpBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流管理控制器
 * 
 * 功能：
 * 1. 查看限流统计数据（今日/历史限流次数）
 * 2. 查看最近限流事件
 * 3. IP黑名单管理（增删查）
 * 4. IP白名单管理（增删查）
 * 5. 查看黑名单操作日志
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/rate-limit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRateLimitController {
    
    private final IpBlacklistService ipBlacklistService;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // ==================== 限流统计 ====================
    
    /**
     * 获取限流概览统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getRateLimitStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        // 今日限流统计
        String today = LocalDate.now().toString();
        String todayStatsKey = RedisKeyPrefix.RATE_LIMIT + "stats:" + today;
        Object todayTotal = redisTemplate.opsForHash().get(todayStatsKey, "total");
        stats.put("todayBlocked", todayTotal != null ? Long.parseLong(todayTotal.toString()) : 0L);
        
        // 最近7天每日限流次数
        List<Map<String, Object>> dailyStats = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            String dateStr = date.toString();
            String statsKey = RedisKeyPrefix.RATE_LIMIT + "stats:" + dateStr;
            Object total = redisTemplate.opsForHash().get(statsKey, "total");
            Map<String, Object> dayStat = new LinkedHashMap<>();
            dayStat.put("date", dateStr);
            dayStat.put("count", total != null ? Long.parseLong(total.toString()) : 0L);
            dailyStats.add(dayStat);
        }
        stats.put("dailyStats", dailyStats);
        
        // 黑名单IP数量
        Long blacklistSize = redisTemplate.opsForSet().size(RedisKeyPrefix.RATE_LIMIT + "blacklist");
        stats.put("blacklistCount", blacklistSize != null ? blacklistSize : 0L);
        
        // 白名单IP数量
        Long whitelistSize = redisTemplate.opsForSet().size(RedisKeyPrefix.RATE_LIMIT + "whitelist");
        stats.put("whitelistCount", whitelistSize != null ? whitelistSize : 0L);
        
        // 今日各接口限流详情
        Map<Object, Object> todayDetails = redisTemplate.opsForHash().entries(todayStatsKey);
        List<Map<String, Object>> apiStats = new ArrayList<>();
        if (todayDetails != null) {
            todayDetails.forEach((key, value) -> {
                if (!"total".equals(key.toString())) {
                    Map<String, Object> apiStat = new LinkedHashMap<>();
                    apiStat.put("api", key.toString());
                    apiStat.put("count", Long.parseLong(value.toString()));
                    apiStats.add(apiStat);
                }
            });
            // 按限流次数降序排列
            apiStats.sort((a, b) -> Long.compare((Long) b.get("count"), (Long) a.get("count")));
        }
        stats.put("apiStats", apiStats);
        
        return Result.success(stats);
    }
    
    /**
     * 获取最近的限流事件
     */
    @GetMapping("/events")
    public Result<List<Map<String, String>>> getRecentEvents(
            @RequestParam(defaultValue = "50") int limit) {
        String eventsKey = RedisKeyPrefix.RATE_LIMIT + "events";
        Set<Object> events = redisTemplate.opsForZSet().reverseRange(eventsKey, 0, limit - 1);
        
        List<Map<String, String>> result = new ArrayList<>();
        if (events != null) {
            for (Object event : events) {
                String[] parts = event.toString().split("\\|", 3);
                if (parts.length >= 3) {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("timestamp", parts[0]);
                    map.put("key", parts[1]);
                    map.put("ip", parts[2]);
                    result.add(map);
                }
            }
        }
        return Result.success(result);
    }
    
    // ==================== IP黑名单管理 ====================
    
    /**
     * 获取黑名单列表
     */
    @GetMapping("/blacklist")
    public Result<Set<Object>> getBlacklist() {
        return Result.success(ipBlacklistService.getBlacklist());
    }
    
    /**
     * 添加IP到黑名单
     */
    @PostMapping("/blacklist")
    @Log(module = "限流管理", operationType = "CREATE", description = "添加IP到黑名单")
    public Result<Void> addToBlacklist(@RequestBody Map<String, String> request) {
        String ip = request.get("ip");
        String reason = request.getOrDefault("reason", "管理员手动添加");
        String duration = request.get("duration"); // 可选，单位分钟
        
        if (ip == null || ip.trim().isEmpty()) {
            return Result.paramError("IP地址不能为空");
        }
        
        ip = ip.trim();
        
        if (duration != null && !duration.isEmpty()) {
            long minutes = Long.parseLong(duration);
            ipBlacklistService.addToBlacklistTemp(ip, minutes, TimeUnit.MINUTES, reason);
        } else {
            ipBlacklistService.addToBlacklist(ip, reason);
        }
        
        return Result.success();
    }
    
    /**
     * 从黑名单移除IP
     */
    @DeleteMapping("/blacklist/{ip}")
    @Log(module = "限流管理", operationType = "DELETE", description = "从黑名单移除IP")
    public Result<Void> removeFromBlacklist(@PathVariable String ip) {
        ipBlacklistService.removeFromBlacklist(ip);
        return Result.success();
    }
    
    // ==================== IP白名单管理 ====================
    
    /**
     * 获取白名单列表
     */
    @GetMapping("/whitelist")
    public Result<Set<Object>> getWhitelist() {
        return Result.success(ipBlacklistService.getWhitelist());
    }
    
    /**
     * 添加IP到白名单
     */
    @PostMapping("/whitelist")
    @Log(module = "限流管理", operationType = "CREATE", description = "添加IP到白名单")
    public Result<Void> addToWhitelist(@RequestBody Map<String, String> request) {
        String ip = request.get("ip");
        if (ip == null || ip.trim().isEmpty()) {
            return Result.paramError("IP地址不能为空");
        }
        ipBlacklistService.addToWhitelist(ip.trim());
        return Result.success();
    }
    
    /**
     * 从白名单移除IP
     */
    @DeleteMapping("/whitelist/{ip}")
    @Log(module = "限流管理", operationType = "DELETE", description = "从白名单移除IP")
    public Result<Void> removeFromWhitelist(@PathVariable String ip) {
        ipBlacklistService.removeFromWhitelist(ip);
        return Result.success();
    }
    
    // ==================== 操作日志 ====================
    
    /**
     * 获取黑名单操作日志
     */
    @GetMapping("/blacklist/log")
    public Result<List<Map<String, String>>> getBlacklistLog() {
        return Result.success(ipBlacklistService.getBlacklistLog());
    }
}
