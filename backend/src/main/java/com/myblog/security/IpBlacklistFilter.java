package com.myblog.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.myblog.common.result.Result;
import com.myblog.service.IpBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * IP黑名单过滤器
 * 
 * 设计目标：在请求进入Controller之前拦截黑名单IP
 * 位于Filter链中，在JwtAuthenticationFilter之前执行
 * 
 * 性能考虑：
 * - Redis SET的 SISMEMBER 操作时间复杂度 O(1)
 * - 只读操作，不会造成Redis写压力
 * - 白名单优先检查，减少不必要的黑名单查询
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IpBlacklistFilter extends OncePerRequestFilter {
    
    private final IpBlacklistService ipBlacklistService;
    private final ObjectMapper objectMapper;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                     HttpServletResponse response, 
                                     FilterChain filterChain) throws ServletException, IOException {
        
        String clientIp = getClientIp(request);
        
        if (ipBlacklistService.isBlacklisted(clientIp)) {
            log.warn("黑名单IP请求被拦截: {}, URI: {}", clientIp, request.getRequestURI());
            
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            
            Result<?> result = Result.error(7002, "您的IP已被限制访问");
            response.getWriter().write(objectMapper.writeValueAsString(result));
            return;
        }
        
        // 设置限流响应头（如果有）
        filterChain.doFilter(request, response);
        
        // 在响应完成后写入限流头信息
        String rateLimitHeader = (String) request.getAttribute("X-RateLimit-Limit");
        if (rateLimitHeader != null) {
            response.setHeader("X-RateLimit-Limit", rateLimitHeader);
            response.setHeader("X-RateLimit-Remaining", 
                    (String) request.getAttribute("X-RateLimit-Remaining"));
            response.setHeader("X-RateLimit-Reset", 
                    (String) request.getAttribute("X-RateLimit-Reset"));
        }
    }
    
    /**
     * 获取客户端真实IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
