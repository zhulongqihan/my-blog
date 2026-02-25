package com.myblog.common.aspect;

import com.myblog.common.annotation.RateLimit;
import com.myblog.common.constant.RedisKeyPrefix;
import com.myblog.common.exception.BusinessException;
import com.myblog.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 
 * 设计亮点：
 * 1. 使用Redis + Lua脚本实现原子操作，保证高并发下计数准确
 * 2. 采用滑动窗口算法，比固定窗口更精确
 * 3. 支持多种限流维度（IP/用户/IP+API）
 * 4. 通过AOP注解方式，对业务代码零侵入
 * 5. 限流结果写入响应头，方便客户端感知
 * 
 * 面试考点：
 * - 为什么用Lua脚本？保证原子性，避免竞态条件
 * - 滑动窗口 vs 固定窗口？滑动窗口避免临界突发
 * - 为什么用ZSET？支持按时间范围高效查询和清理
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {
    
    private final RedisTemplate<String, Object> redisTemplate;
    
    private DefaultRedisScript<List<Long>> rateLimitScript;
    
    /**
     * 初始化Lua脚本
     * 在Bean创建后加载脚本，避免每次请求都读取文件
     */
    @PostConstruct
    public void init() {
        rateLimitScript = new DefaultRedisScript<>();
        rateLimitScript.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("scripts/rate_limit.lua")));
        rateLimitScript.setResultType((Class) List.class);
    }
    
    /**
     * 环绕通知：拦截所有标注@RateLimit的方法
     */
    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        // 1. 构建限流Key
        String rateLimitKey = buildRateLimitKey(rateLimit, joinPoint);
        
        // 2. 计算时间窗口（转为毫秒）
        long windowMillis = rateLimit.timeUnit().toMillis(rateLimit.timeWindow());
        long now = System.currentTimeMillis();
        long windowStart = now - windowMillis;
        
        // 窗口过期时间（秒），Key的TTL设为窗口大小的2倍，确保数据自动清理
        long expireSeconds = rateLimit.timeUnit().toSeconds(rateLimit.timeWindow()) * 2;
        if (expireSeconds < 1) expireSeconds = 1;
        
        // 3. 执行Lua脚本进行限流判断
        List<Long> result = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(rateLimitKey),
                now, windowStart, (long) rateLimit.maxRequests(), expireSeconds
        );
        
        if (result == null || result.isEmpty()) {
            log.error("限流脚本执行失败，Key: {}", rateLimitKey);
            // 限流脚本异常时放行，遵循 fail-open 策略
            return joinPoint.proceed();
        }
        
        long blocked = result.get(0);
        long currentCount = result.get(1);
        long remaining = result.get(2);
        
        // 4. 设置限流响应头（供客户端感知）
        setRateLimitHeaders(rateLimit.maxRequests(), remaining, windowMillis);
        
        // 5. 判断是否被限流
        if (blocked == 1) {
            log.warn("接口限流触发 - Key: {}, 当前请求数: {}/{}, 窗口: {}s",
                    rateLimitKey, currentCount, rateLimit.maxRequests(),
                    rateLimit.timeUnit().toSeconds(rateLimit.timeWindow()));
            
            // 记录限流事件到Redis（供后台监控页面查询）
            recordRateLimitEvent(rateLimitKey, rateLimit);
            
            throw new BusinessException(7001, rateLimit.message());
        }
        
        if (log.isDebugEnabled()) {
            log.debug("限流检查通过 - Key: {}, 当前: {}/{}, 剩余: {}",
                    rateLimitKey, currentCount, rateLimit.maxRequests(), remaining);
        }
        
        // 6. 放行
        return joinPoint.proceed();
    }
    
    /**
     * 构建限流Key
     * 
     * 格式：rate:limit:{limitType}:{identifier}:{api}
     * 示例：rate:limit:ip:192.168.1.1:POST:/api/auth/login
     */
    private String buildRateLimitKey(RateLimit rateLimit, ProceedingJoinPoint joinPoint) {
        StringBuilder key = new StringBuilder(RedisKeyPrefix.RATE_LIMIT);
        
        switch (rateLimit.limitType()) {
            case IP:
                key.append("ip:").append(getClientIp());
                break;
            case USER:
                key.append("user:").append(getCurrentUserId());
                break;
            case IP_AND_API:
                key.append("ip_api:").append(getClientIp());
                break;
        }
        
        // 添加方法标识
        String apiKey = rateLimit.prefix().isEmpty() 
                ? getMethodSignature(joinPoint)
                : rateLimit.prefix();
        key.append(":").append(apiKey);
        
        return key.toString();
    }
    
    /**
     * 获取方法签名作为API标识
     */
    private String getMethodSignature(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 格式：类名.方法名
        return method.getDeclaringClass().getSimpleName() + "." + method.getName();
    }
    
    /**
     * 获取客户端真实IP
     * 支持Nginx/Apache等反向代理场景
     */
    private String getClientIp() {
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) return "unknown";
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个（真实客户端IP）
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
    
    /**
     * 获取当前登录用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof User user) {
                return String.valueOf(user.getId());
            }
        } catch (Exception ignored) {
        }
        // 未登录时回退到 IP 限流
        return "anonymous:" + getClientIp();
    }
    
    /**
     * 设置限流响应头
     * X-RateLimit-Limit: 允许的最大请求数
     * X-RateLimit-Remaining: 剩余请求数
     * X-RateLimit-Reset: 窗口重置时间（毫秒）
     */
    private void setRateLimitHeaders(int maxRequests, long remaining, long windowMillis) {
        HttpServletRequest request = getHttpServletRequest();
        if (request != null) {
            // 通过request attribute传递，由Filter或Interceptor写入响应头
            request.setAttribute("X-RateLimit-Limit", String.valueOf(maxRequests));
            request.setAttribute("X-RateLimit-Remaining", String.valueOf(Math.max(0, remaining)));
            request.setAttribute("X-RateLimit-Reset", String.valueOf(windowMillis / 1000));
        }
    }
    
    /**
     * 记录限流事件到Redis
     * 用于后台监控页面展示限流统计
     * 
     * 数据结构：
     * - rate:limit:events (ZSET) - 限流事件时间线
     * - rate:limit:stats:{date} (HASH) - 每日限流统计
     */
    private void recordRateLimitEvent(String rateLimitKey, RateLimit rateLimit) {
        try {
            String today = java.time.LocalDate.now().toString();
            String statsKey = RedisKeyPrefix.RATE_LIMIT + "stats:" + today;
            
            // 增加当日限流次数统计
            redisTemplate.opsForHash().increment(statsKey, "total", 1);
            redisTemplate.opsForHash().increment(statsKey, rateLimitKey, 1);
            
            // 设置统计Key的过期时间（保留30天）
            redisTemplate.expire(statsKey, 30, TimeUnit.DAYS);
            
            // 记录最近的限流事件（保留最近1000条）
            String eventsKey = RedisKeyPrefix.RATE_LIMIT + "events";
            String event = System.currentTimeMillis() + "|" + rateLimitKey + "|" + getClientIp();
            redisTemplate.opsForZSet().add(eventsKey, event, System.currentTimeMillis());
            
            // 只保留最近1000条事件
            long count = redisTemplate.opsForZSet().zCard(eventsKey);
            if (count > 1000) {
                redisTemplate.opsForZSet().removeRange(eventsKey, 0, count - 1001);
            }
        } catch (Exception e) {
            log.warn("记录限流事件失败", e);
        }
    }
    
    private HttpServletRequest getHttpServletRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) 
                    RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
