package com.myblog.service;

import com.myblog.common.constant.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * IP黑名单服务
 * 
 * 设计目标：
 * 1. 支持手动拉黑IP（管理员操作）
 * 2. 支持自动拉黑（限流触发阈值后自动拉黑）
 * 3. 支持IP白名单（白名单IP不受限流和拉黑影响）
 * 4. 支持黑名单过期（临时封禁）
 * 
 * 数据结构：
 * - rate:limit:blacklist (SET) - 永久黑名单
 * - rate:limit:blacklist:temp:{ip} (STRING) - 临时黑名单（带TTL）
 * - rate:limit:whitelist (SET) - 白名单
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpBlacklistService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private static final String BLACKLIST_KEY = RedisKeyPrefix.RATE_LIMIT + "blacklist";
    private static final String BLACKLIST_TEMP_PREFIX = RedisKeyPrefix.RATE_LIMIT + "blacklist:temp:";
    private static final String WHITELIST_KEY = RedisKeyPrefix.RATE_LIMIT + "whitelist";
    private static final String BLACKLIST_LOG_KEY = RedisKeyPrefix.RATE_LIMIT + "blacklist:log";
    
    /**
     * 检查IP是否在黑名单中
     */
    public boolean isBlacklisted(String ip) {
        if (ip == null) return false;
        
        // 先检查白名单（白名单优先级最高）
        if (isWhitelisted(ip)) return false;
        
        // 检查永久黑名单
        Boolean isPermanent = redisTemplate.opsForSet().isMember(BLACKLIST_KEY, ip);
        if (Boolean.TRUE.equals(isPermanent)) return true;
        
        // 检查临时黑名单
        Boolean isTemp = redisTemplate.hasKey(BLACKLIST_TEMP_PREFIX + ip);
        return Boolean.TRUE.equals(isTemp);
    }
    
    /**
     * 检查IP是否在白名单中
     */
    public boolean isWhitelisted(String ip) {
        Boolean isMember = redisTemplate.opsForSet().isMember(WHITELIST_KEY, ip);
        return Boolean.TRUE.equals(isMember);
    }
    
    /**
     * 添加IP到黑名单（永久）
     */
    public void addToBlacklist(String ip, String reason) {
        redisTemplate.opsForSet().add(BLACKLIST_KEY, ip);
        logBlacklistAction(ip, "ADD_PERMANENT", reason);
        log.info("IP已加入永久黑名单: {}, 原因: {}", ip, reason);
    }
    
    /**
     * 添加IP到临时黑名单
     * @param ip IP地址
     * @param duration 封禁时长
     * @param unit 时间单位
     * @param reason 封禁原因
     */
    public void addToBlacklistTemp(String ip, long duration, TimeUnit unit, String reason) {
        String key = BLACKLIST_TEMP_PREFIX + ip;
        redisTemplate.opsForValue().set(key, reason, duration, unit);
        logBlacklistAction(ip, "ADD_TEMP_" + duration + unit.name(), reason);
        log.info("IP已加入临时黑名单: {}, 时长: {}{}, 原因: {}", ip, duration, unit, reason);
    }
    
    /**
     * 从黑名单中移除IP
     */
    public void removeFromBlacklist(String ip) {
        redisTemplate.opsForSet().remove(BLACKLIST_KEY, ip);
        redisTemplate.delete(BLACKLIST_TEMP_PREFIX + ip);
        logBlacklistAction(ip, "REMOVE", "管理员手动移除");
        log.info("IP已从黑名单移除: {}", ip);
    }
    
    /**
     * 添加IP到白名单
     */
    public void addToWhitelist(String ip) {
        redisTemplate.opsForSet().add(WHITELIST_KEY, ip);
        log.info("IP已加入白名单: {}", ip);
    }
    
    /**
     * 从白名单中移除IP
     */
    public void removeFromWhitelist(String ip) {
        redisTemplate.opsForSet().remove(WHITELIST_KEY, ip);
        log.info("IP已从白名单移除: {}", ip);
    }
    
    /**
     * 获取所有黑名单IP
     */
    public Set<Object> getBlacklist() {
        Set<Object> result = redisTemplate.opsForSet().members(BLACKLIST_KEY);
        return result != null ? result : Collections.emptySet();
    }
    
    /**
     * 获取所有白名单IP
     */
    public Set<Object> getWhitelist() {
        Set<Object> result = redisTemplate.opsForSet().members(WHITELIST_KEY);
        return result != null ? result : Collections.emptySet();
    }
    
    /**
     * 获取黑名单操作日志（最近100条）
     */
    public List<Map<String, String>> getBlacklistLog() {
        Set<Object> logs = redisTemplate.opsForZSet().reverseRange(BLACKLIST_LOG_KEY, 0, 99);
        List<Map<String, String>> result = new ArrayList<>();
        if (logs != null) {
            for (Object logEntry : logs) {
                String[] parts = logEntry.toString().split("\\|", 4);
                if (parts.length >= 4) {
                    Map<String, String> map = new LinkedHashMap<>();
                    map.put("time", parts[0]);
                    map.put("ip", parts[1]);
                    map.put("action", parts[2]);
                    map.put("reason", parts[3]);
                    result.add(map);
                }
            }
        }
        return result;
    }
    
    /**
     * 记录黑名单操作日志
     */
    private void logBlacklistAction(String ip, String action, String reason) {
        try {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String entry = time + "|" + ip + "|" + action + "|" + reason;
            redisTemplate.opsForZSet().add(BLACKLIST_LOG_KEY, entry, System.currentTimeMillis());
            
            // 只保留最近500条日志
            Long count = redisTemplate.opsForZSet().zCard(BLACKLIST_LOG_KEY);
            if (count != null && count > 500) {
                redisTemplate.opsForZSet().removeRange(BLACKLIST_LOG_KEY, 0, count - 501);
            }
            // 30天过期
            redisTemplate.expire(BLACKLIST_LOG_KEY, 30, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("记录黑名单日志失败", e);
        }
    }
}
