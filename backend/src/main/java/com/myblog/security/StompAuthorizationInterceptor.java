package com.myblog.security;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class StompAuthorizationInterceptor implements ChannelInterceptor {
    private static final String SESSION_TOKEN =
            StompAuthorizationInterceptor.class.getName() + ".token";
    private final JwtUtils jwt;
    private final JwtBlacklistService blacklist;
    private final UserDetailsService users;
    // Only authenticated, live sessions are tracked; anonymous connections create no entries.
    private final ConcurrentHashMap<String, String> authenticatedSessions =
            new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headers =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (headers == null) return message;
        StompCommand command = headers.getCommand();
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            String authorization = headers.getFirstNativeHeader("Authorization");
            if (authorization == null && headers.getSessionId() != null)
                authenticatedSessions.remove(headers.getSessionId());
            if (authorization != null) {
                if (!authorization.startsWith("Bearer ")) throw denied();
                String token = authorization.substring(7);
                Authentication authentication = authenticate(token);
                if (headers.getSessionAttributes() == null)
                    headers.setSessionAttributes(new HashMap<>());
                headers.getSessionAttributes().put(SESSION_TOKEN, token);
                headers.setUser(authentication);
                if (headers.getSessionId() != null)
                    authenticatedSessions.put(headers.getSessionId(), token);
            }
        } else if (command == StompCommand.SUBSCRIBE) {
            String destination = headers.getDestination();
            if ("/topic/online-count".equals(destination)
                    || "/topic/notifications".equals(destination)) return message;
            Authentication authentication = currentAuthentication(headers);
            if ("/topic/admin/notifications".equals(destination)) {
                if (authentication.getAuthorities().stream()
                        .noneMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))) throw denied();
            } else if (!"/user/queue/notifications".equals(destination)) {
                throw denied();
            }
        } else if (command == StompCommand.DISCONNECT) {
            if (headers.getSessionId() != null)
                authenticatedSessions.remove(headers.getSessionId());
        } else if (command == StompCommand.SEND) {
            // The application currently exposes no client-originated messaging commands.
            // Clients must never publish directly into broker topics or another user's queue.
            throw denied();
        } else if (command != null
                && command != StompCommand.DISCONNECT
                && command != StompCommand.UNSUBSCRIBE
                && command != StompCommand.ACK
                && command != StompCommand.NACK) {
            throw denied();
        }
        return message;
    }

    public Message<?> preSendOutbound(Message<?> message) {
        if (SimpMessageHeaderAccessor.getMessageType(message.getHeaders())
                != SimpMessageType.MESSAGE) return message;
        String destination = SimpMessageHeaderAccessor.getDestination(message.getHeaders());
        if ("/topic/online-count".equals(destination) || "/topic/notifications".equals(destination))
            return message;
        String sessionId = SimpMessageHeaderAccessor.getSessionId(message.getHeaders());
        if (sessionId == null) return null;
        String token = authenticatedSessions.get(sessionId);
        if (token == null) return null;
        try {
            Authentication authentication = authenticate(token);
            if ("/topic/admin/notifications".equals(destination)) {
                return authentication.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()))
                        ? message
                        : null;
            }
            return ("/queue/notifications-user" + sessionId).equals(destination) ? message : null;
        } catch (AccessDeniedException exception) {
            authenticatedSessions.remove(sessionId, token);
            return null;
        }
    }

    @EventListener
    public void disconnected(SessionDisconnectEvent event) {
        authenticatedSessions.remove(event.getSessionId());
    }

    int authenticatedSessionCount() {
        return authenticatedSessions.size();
    }

    private Authentication currentAuthentication(StompHeaderAccessor headers) {
        if (headers.getUser() == null || headers.getSessionAttributes() == null) throw denied();
        Object token = headers.getSessionAttributes().get(SESSION_TOKEN);
        if (!(token instanceof String value)) throw denied();
        Authentication authentication = authenticate(value);
        if (!authentication.getName().equals(headers.getUser().getName())) throw denied();
        return authentication;
    }

    private Authentication authenticate(String token) {
        try {
            if (!jwt.validateToken(token) || blacklist.isBlacklisted(token)) throw denied();
            UserDetails user = users.loadUserByUsername(jwt.getUsernameFromToken(token));
            if (!user.isEnabled()
                    || !user.isAccountNonLocked()
                    || !user.isAccountNonExpired()
                    || !user.isCredentialsNonExpired()) throw denied();
            return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        } catch (RuntimeException exception) {
            throw denied();
        }
    }

    private AccessDeniedException denied() {
        return new AccessDeniedException("该消息操作未获授权");
    }
}
