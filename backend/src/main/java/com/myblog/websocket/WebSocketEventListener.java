package com.myblog.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 连接事件监听器
 * 
 * 监听 STOMP 连接/断开事件，实现：
 * 1. 实时在线人数统计（AtomicInteger 原子计数）
 * 2. 在线人数变化时广播到 /topic/online-count
 * 3. 已认证用户的 Session 映射管理
 * 
 * 面试亮点：
 * - Spring 事件机制（@EventListener）
 * - ConcurrentHashMap + AtomicInteger 线程安全
 * - 广播模式实时推送在线人数
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    /** 当前在线连接数（原子操作，线程安全） */
    private final AtomicInteger onlineCount = new AtomicInteger(0);

    /** sessionId → username 映射（用于断开时识别用户） */
    private final ConcurrentHashMap<String, String> sessionUserMap = new ConcurrentHashMap<>();

    /**
     * WebSocket 连接建立事件
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        int count = onlineCount.incrementAndGet();
        
        // 如果是已认证用户，记录映射
        if (accessor.getUser() != null) {
            String username = accessor.getUser().getName();
            sessionUserMap.put(sessionId, username);
            log.info("[WebSocket] 用户上线: {}，当前在线: {}", username, count);
        } else {
            log.info("[WebSocket] 匿名用户连接，当前在线: {}", count);
        }
        
        // 广播在线人数
        broadcastOnlineCount(count);
    }

    /**
     * WebSocket 连接断开事件
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        
        int count = onlineCount.decrementAndGet();
        if (count < 0) {
            onlineCount.set(0);
            count = 0;
        }
        
        // 移除用户映射
        String username = sessionUserMap.remove(sessionId);
        if (username != null) {
            log.info("[WebSocket] 用户下线: {}，当前在线: {}", username, count);
        } else {
            log.info("[WebSocket] 匿名用户断开，当前在线: {}", count);
        }
        
        // 广播在线人数
        broadcastOnlineCount(count);
    }

    /**
     * 广播当前在线人数到所有客户端
     */
    private void broadcastOnlineCount(int count) {
        messagingTemplate.convertAndSend("/topic/online-count", 
                Map.of("onlineCount", count, "timestamp", System.currentTimeMillis()));
    }

    /**
     * 获取当前在线人数
     */
    public int getOnlineCount() {
        return onlineCount.get();
    }

    /**
     * 获取所有在线用户
     */
    public Map<String, String> getOnlineUsers() {
        return Map.copyOf(sessionUserMap);
    }
}
