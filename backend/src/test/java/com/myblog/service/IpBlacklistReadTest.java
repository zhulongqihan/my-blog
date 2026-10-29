package com.myblog.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import com.myblog.config.RedisConfig;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.connection.*;

class IpBlacklistReadTest {
    private static final String IP = "198.51.100.17";

    @SuppressWarnings("unchecked")
    private RedisTemplate<String, Object> redis() {
        return mock(RedisTemplate.class, RETURNS_DEEP_STUBS);
    }

    @Test void aBlockedIpIsCheckedWithOneAtomicRedisOperation() {
        var redis = redis();
        when(redis.execute(any(RedisScript.class), anyList(), eq(IP))).thenReturn(1L);
        assertThat(new IpBlacklistService(redis).isBlacklisted(IP)).isTrue();
        verify(redis).execute(any(RedisScript.class), eq(List.of(
                "rate:limit:whitelist", "rate:limit:blacklist", "rate:limit:blacklist:temp:" + IP)), eq(IP));
        verify(redis, never()).opsForSet();
        verify(redis, never()).hasKey(anyString());
    }

    @Test void anAllowedIpStillNeedsOneFreshServerDecision() {
        var redis = redis();
        when(redis.execute(any(RedisScript.class), anyList(), eq(IP))).thenReturn(0L);
        var service = new IpBlacklistService(redis);
        assertThat(service.isBlacklisted(IP)).isFalse();
        assertThat(service.isBlacklisted(IP)).isFalse();
        verify(redis, times(2)).execute(any(RedisScript.class), anyList(), eq(IP));
        verify(redis, never()).opsForSet();
    }

    @Test void anUnavailableOrIndeterminateDecisionCannotSilentlyAllowAccess() {
        var redis = redis();
        when(redis.execute(any(RedisScript.class), anyList(), eq(IP)))
                .thenThrow(new RedisConnectionFailureException("isolated unavailable"));
        assertThatThrownBy(() -> new IpBlacklistService(redis).isBlacklisted(IP))
                .isInstanceOf(RedisConnectionFailureException.class);
        reset(redis);
        when(redis.execute(any(RedisScript.class), anyList(), eq(IP))).thenReturn(null);
        assertThatThrownBy(() -> new IpBlacklistService(redis).isBlacklisted(IP))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test void absentIpDoesNotContactRedis() {
        var redis = redis();
        assertThat(new IpBlacklistService(redis).isBlacklisted(null)).isFalse();
        verifyNoInteractions(redis);
    }

    @SuppressWarnings("unchecked")
    @Test void configuredScriptArgumentsMatchPreviouslyStoredJsonSetMembers() {
        var factory = mock(RedisConnectionFactory.class);
        var connection = mock(RedisConnection.class);
        when(factory.getConnection()).thenReturn(connection);
        var encoded = new AtomicReference<byte[][]>();
        when(connection.evalSha(anyString(), eq(ReturnType.INTEGER), eq(3), any(byte[][].class)))
                .thenAnswer(invocation -> {
                    encoded.set((byte[][]) invocation.getRawArguments()[3]);
                    return 1L;
                });
        var configured = new RedisConfig().redisTemplate(factory);
        assertThat(new IpBlacklistService(configured).isBlacklisted(IP)).isTrue();
        assertThat(encoded.get().length).isEqualTo(4);
        assertThat(new String(encoded.get()[0], StandardCharsets.UTF_8)).isEqualTo("rate:limit:whitelist");
        assertThat(new String(encoded.get()[2], StandardCharsets.UTF_8))
                .isEqualTo("rate:limit:blacklist:temp:" + IP);
        var serializer = (org.springframework.data.redis.serializer.RedisSerializer<Object>) configured.getValueSerializer();
        assertThat(encoded.get()[3]).isEqualTo(serializer.serialize(IP));
        assertThat(new String(encoded.get()[3], StandardCharsets.UTF_8)).isEqualTo("\"" + IP + "\"");
    }
}
