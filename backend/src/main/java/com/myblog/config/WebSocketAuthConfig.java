package com.myblog.config;

import com.myblog.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 安全认证配置
 * 
 * 在 STOMP CONNECT 握手阶段解析 JWT Token，
 * 将认证信息绑定到 WebSocket Session 上，
 * 实现已登录用户的身份识别和点对点推送。
 * 
 * 面试亮点：
 * - ChannelInterceptor 拦截 STOMP CONNECT 帧
 * - 复用已有 JWT 认证机制（JwtUtils）
 * - 未认证用户也能连接（接收广播），但不能收私信
 */
@Slf4j
@Configuration
@EnableWebSocketMessageBroker
@Order(Ordered.HIGHEST_PRECEDENCE + 99)
@RequiredArgsConstructor
public class WebSocketAuthConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtils jwtUtils;
    private final UserDetailsService userDetailsService;

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor
                        .getAccessor(message, StompHeaderAccessor.class);
                
                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 从 STOMP CONNECT 帧的 header 中提取 JWT Token
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        try {
                            if (jwtUtils.validateToken(token)) {
                                String username = jwtUtils.getUsernameFromToken(token);
                                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                                UsernamePasswordAuthenticationToken auth = 
                                        new UsernamePasswordAuthenticationToken(
                                                userDetails, null, userDetails.getAuthorities());
                                SecurityContextHolder.getContext().setAuthentication(auth);
                                accessor.setUser(auth);
                                log.info("[WebSocket] 用户认证成功: {}", username);
                            }
                        } catch (Exception e) {
                            log.warn("[WebSocket] JWT认证失败: {}", e.getMessage());
                        }
                    } else {
                        log.debug("[WebSocket] 匿名用户连接（无JWT）");
                    }
                }
                return message;
            }
        });
    }
}
