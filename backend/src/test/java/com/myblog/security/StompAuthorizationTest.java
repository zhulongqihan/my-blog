package com.myblog.security;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.myblog.entity.User;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.HashMap;

@ExtendWith(MockitoExtension.class)
class StompAuthorizationTest {
    @Mock JwtUtils jwt;
    @Mock JwtBlacklistService blacklist;
    @Mock UserDetailsService users;
    @InjectMocks StompAuthorizationInterceptor interceptor;

    private Message<byte[]> frame(StompHeaderAccessor headers) {
        headers.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], headers.getMessageHeaders());
    }

    private StompHeaderAccessor subscription(String destination) {
        var h = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        h.setDestination(destination);
        return h;
    }

    private StompHeaderAccessor connect(User.Role role) {
        when(jwt.validateToken("good")).thenReturn(true);
        when(jwt.getUsernameFromToken("good")).thenReturn("reader");
        when(users.loadUserByUsername("reader"))
                .thenReturn(User.builder().id(1L).username("reader").role(role).build());
        var h = StompHeaderAccessor.create(StompCommand.CONNECT);
        h.setSessionId("test-session");
        h.setSessionAttributes(new HashMap<>());
        h.setNativeHeader("Authorization", "Bearer good");
        interceptor.preSend(frame(h), null);
        return h;
    }

    private StompHeaderAccessor as(StompHeaderAccessor connection, String destination) {
        var h = subscription(destination);
        h.setUser(connection.getUser());
        h.setSessionAttributes(connection.getSessionAttributes());
        return h;
    }

    @Test
    void anonymousCanStillReadPublicTopics() {
        assertThat(interceptor.preSend(frame(subscription("/topic/online-count")), null))
                .isNotNull();
        assertThat(interceptor.preSend(frame(subscription("/topic/notifications")), null))
                .isNotNull();
    }

    @Test
    void anonymousCannotSubscribeAdminOrUserQueues() {
        for (String d :
                new String[] {
                    "/topic/admin/notifications",
                    "/user/queue/notifications",
                    "/queue/notifications",
                    "/topic/**"
                })
            assertThatThrownBy(() -> interceptor.preSend(frame(subscription(d)), null))
                    .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void regularUserCanReadOwnQueueButNotAdminTopic() {
        var c = connect(User.Role.USER);
        interceptor.preSend(frame(as(c, "/user/queue/notifications")), null);
        assertThatThrownBy(
                        () -> interceptor.preSend(frame(as(c, "/topic/admin/notifications")), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void adminReceivesBothPublicAndAdministrativeMessages() {
        var c = connect(User.Role.ADMIN);
        interceptor.preSend(frame(as(c, "/topic/admin/notifications")), null);
        interceptor.preSend(frame(as(c, "/topic/notifications")), null);
        interceptor.preSend(frame(as(c, "/user/queue/notifications")), null);
    }

    @Test
    void clientsCannotForgeBrokerMessages() {
        var h = StompHeaderAccessor.create(StompCommand.SEND);
        h.setDestination("/topic/notifications");
        assertThatThrownBy(() -> interceptor.preSend(frame(h), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void invalidSuppliedTokenIsRejectedInsteadOfAnonymousDowngrade() {
        var h = StompHeaderAccessor.create(StompCommand.CONNECT);
        h.setNativeHeader("Authorization", "Bearer invalid");
        assertThatThrownBy(() -> interceptor.preSend(frame(h), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void revokedTokenCannotSubscribeAfterConnecting() {
        var c = connect(User.Role.ADMIN);
        when(blacklist.isBlacklisted("good")).thenReturn(true);
        assertThatThrownBy(
                        () -> interceptor.preSend(frame(as(c, "/topic/admin/notifications")), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void accountDisabledBetweenConnectAndSubscribeIsRejected() {
        var c = connect(User.Role.ADMIN);
        when(users.loadUserByUsername("reader"))
                .thenReturn(
                        User.builder().username("reader").role(User.Role.ADMIN).status(0).build());
        assertThatThrownBy(
                        () -> interceptor.preSend(frame(as(c, "/topic/admin/notifications")), null))
                .isInstanceOf(AccessDeniedException.class);
    }

    private Message<byte[]> privateDelivery(String destination) {
        var headers = StompHeaderAccessor.create(StompCommand.MESSAGE);
        headers.setSessionId("test-session");
        headers.setDestination(destination);
        return frame(headers);
    }

    @Test
    void accountDisabledAfterSubscribingStopsReceivingPrivateMessages() {
        var connection = connect(User.Role.ADMIN);
        interceptor.preSend(frame(as(connection, "/topic/admin/notifications")), null);
        var message = privateDelivery("/topic/admin/notifications");
        assertThat(interceptor.preSendOutbound(message)).isSameAs(message);
        when(users.loadUserByUsername("reader"))
                .thenReturn(
                        User.builder().username("reader").role(User.Role.ADMIN).status(0).build());
        assertThat(interceptor.preSendOutbound(message)).isNull();
        assertThat(interceptor.authenticatedSessionCount()).isZero();
    }

    @Test
    void logoutRevokesAnAlreadySubscribedConnection() {
        connect(User.Role.ADMIN);
        when(blacklist.isBlacklisted("good")).thenReturn(true);
        assertThat(interceptor.preSendOutbound(privateDelivery("/topic/admin/notifications")))
                .isNull();
    }

    @Test
    void roleDowngradeStopsAdministrativeDeliveryButKeepsOwnQueue() {
        connect(User.Role.ADMIN);
        when(users.loadUserByUsername("reader"))
                .thenReturn(User.builder().username("reader").role(User.Role.USER).build());
        assertThat(interceptor.preSendOutbound(privateDelivery("/topic/admin/notifications")))
                .isNull();
        assertThat(
                        interceptor.preSendOutbound(
                                privateDelivery("/queue/notifications-usertest-session")))
                .isNotNull();
    }

    @Test
    void expiredTokenStopsExistingPrivateDelivery() {
        connect(User.Role.ADMIN);
        when(jwt.validateToken("good")).thenReturn(false);
        assertThat(interceptor.preSendOutbound(privateDelivery("/topic/admin/notifications")))
                .isNull();
    }

    @Test
    void disconnectCleansAuthenticatedSessionsAndUnknownSessionsCannotReceive() {
        connect(User.Role.ADMIN);
        assertThat(interceptor.authenticatedSessionCount()).isEqualTo(1);
        var disconnected = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        disconnected.setSessionId("test-session");
        interceptor.preSend(frame(disconnected), null);
        assertThat(interceptor.authenticatedSessionCount()).isZero();
        assertThat(interceptor.preSendOutbound(privateDelivery("/topic/admin/notifications")))
                .isNull();
    }

    @Test
    void anonymousConnectDoesNotAllocateAuthenticationState() {
        var headers = StompHeaderAccessor.create(StompCommand.CONNECT);
        headers.setSessionId("anonymous");
        interceptor.preSend(frame(headers), null);
        assertThat(interceptor.authenticatedSessionCount()).isZero();
        var message = privateDelivery("/topic/notifications");
        assertThat(interceptor.preSendOutbound(message)).isSameAs(message);
    }

    @Test
    void networkDisconnectEventCleansAuthenticatedSession() {
        connect(User.Role.ADMIN);
        interceptor.disconnected(
                new org.springframework.web.socket.messaging.SessionDisconnectEvent(
                        this,
                        privateDelivery("/topic/admin/notifications"),
                        "test-session",
                        org.springframework.web.socket.CloseStatus.NORMAL));
        assertThat(interceptor.authenticatedSessionCount()).isZero();
    }
}
