package com.myblog.security;

import com.myblog.common.constant.RedisKeyPrefix;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT黑名单服务
 * 设计目标：解决JWT无状态导致的登出问题
 * 
 * 问题：JWT是无状态的，用户登出后Token依然有效
 * 解决方案：使用Redis维护Token黑名单
 * 
 * 工作流程：
 * 1. 用户登出时，将Token加入黑名单（过期时间 = Token剩余有效期）
 * 2. 每次请求时，先检查Token是否在黑名单中
 * 3. 如果在黑名单，拒绝访问
 * 4. Token过期后，Redis自动删除黑名单记录（节省内存）
 * 
 * 性能优化：
 * - 使用Redis的TTL机制，自动清理过期Token
 * - Key设计简洁，减少内存占用
 * - 使用String类型存储，查询速度快
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 将Token加入黑名单
     * 
     * @param token JWT Token
     * @param userId 用户ID
     * @param expirationTime Token过期时间（毫秒时间戳）
     * 
     * 设计意图：
     * - 用户登出时调用此方法
     * - TTL设置为Token剩余有效期，过期后自动删除
     * - 存储用户ID便于日志追踪和统计
     */
    public void addToBlacklist(String token, Long userId, Long expirationTime) {
        String key = RedisKeyPrefix.JWT_BLACKLIST + token;
        long ttl = expirationTime - System.currentTimeMillis();
        
        // 只有Token还未过期才加入黑名单
        if (ttl > 0) {
            redisTemplate.opsForValue().set(key, userId.toString(), ttl, TimeUnit.MILLISECONDS);
            log.info("Token已加入黑名单，用户ID: {}, 剩余有效期: {}ms", userId, ttl);
        } else {
            log.warn("Token已过期，无需加入黑名单，用户ID: {}", userId);
        }
    }
    
    /**
     * 检查Token是否在黑名单中
     * 
     * @param token JWT Token
     * @return true-在黑名单中（已登出），false-不在黑名单中（有效）
     * 
     * 设计意图：
     * - 每次请求都要检查，防止已登出的Token被使用
     * - 使用hasKey而不是get，性能更好
     * - 如果Token在黑名单，说明用户已登出，应拒绝访问
     */
    public boolean isBlacklisted(String token) {
        String key = RedisKeyPrefix.JWT_BLACKLIST + token;
        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }
    
    /**
     * 从黑名单中移除Token（一般不需要，Redis会自动过期）
     * 
     * @param token JWT Token
     * 
     * 使用场景：
     * - 管理员强制恢复某个Token的有效性（极少使用）
     */
    public void removeFromBlacklist(String token) {
        String key = RedisKeyPrefix.JWT_BLACKLIST + token;
        redisTemplate.delete(key);
        log.info("Token已从黑名单移除");
    }
    
    /**
     * 获取黑名单中Token的数量
     * 
     * @return 黑名单Token数量
     * 
     * 使用场景：
     * - 监控和统计
     * - 仪表盘展示
     */
    public Long getBlacklistCount() {
        String pattern = RedisKeyPrefix.JWT_BLACKLIST + "*";
        return Long.valueOf(redisTemplate.keys(pattern).size());
    }
}
