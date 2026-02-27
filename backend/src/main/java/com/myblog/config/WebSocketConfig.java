package com.myblog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 
 * 使用 STOMP 子协议 + SockJS 降级方案
 * 
 * 架构设计：
 * 1. /ws 端点 - WebSocket 连接入口（SockJS 降级到长轮询）
 * 2. /topic 前缀 - 广播模式（如在线人数、全站公告）
 * 3. /queue 前缀 - 点对点模式（如个人通知）
 * 4. /app 前缀 - 客户端发送消息的路由前缀
 * 
 * 面试亮点：
 * - STOMP 协议实现语义化消息传输
 * - SockJS 降级保证浏览器兼容性
 * - 内存消息代理（轻量级，适合单体架构）
 * - 与 Spring Security 集成实现用户身份识别
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 启用简单内存消息代理
        // /topic - 广播目的地（1对多）
        // /queue - 用户队列（1对1，Spring自动加上/user前缀）
        registry.enableSimpleBroker("/topic", "/queue");
        
        // 客户端发送消息的前缀（如 /app/send）
        registry.setApplicationDestinationPrefixes("/app");
        
        // 用户目的地前缀（如 /user/queue/notifications → 发给特定用户）
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 STOMP 端点，启用 SockJS 降级
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // 允许跨域
                .withSockJS();                  // SockJS 降级支持
    }
}
