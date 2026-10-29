package com.myblog.service;

import static org.assertj.core.api.Assertions.*;

import com.myblog.config.RedisConfig;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.*;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

/** Requires an explicitly provisioned local disposable Redis; never connects to the default server. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IpBlacklistLuaIntegrationTest {
    LettuceConnectionFactory factory;
    RedisTemplate<String, Object> redis;
    IpBlacklistService service;
    final String ip = "test-ip-" + UUID.randomUUID();

    @BeforeAll void connect() {
        String configuredPort = System.getProperty("blog.test.redis.port", System.getenv("BLOG_TEST_REDIS_PORT"));
        Assumptions.assumeTrue(configuredPort != null && !configuredPort.isBlank(),
                "An explicit isolated BLOG_TEST_REDIS_PORT is required");
        var configuration = new RedisStandaloneConfiguration("127.0.0.1", Integer.parseInt(configuredPort));
        configuration.setDatabase(15);
        factory = new LettuceConnectionFactory(configuration);
        factory.afterPropertiesSet(); factory.start();
        redis = new RedisConfig().redisTemplate(factory);
        service = new IpBlacklistService(redis);
    }

    @AfterAll void cleanup() {
        if (redis != null) {
            redis.opsForSet().remove("rate:limit:whitelist", ip);
            redis.opsForSet().remove("rate:limit:blacklist", ip);
            redis.delete("rate:limit:blacklist:temp:" + ip);
        }
        if (factory != null) factory.destroy();
    }

    @Test void existingJsonEncodedMembersKeepWhitelistPrecedenceAndImmediateUpdates() {
        assertThat(service.isBlacklisted(ip)).isFalse();
        redis.opsForSet().add("rate:limit:blacklist", ip);
        assertThat(service.isBlacklisted(ip)).isTrue();
        redis.opsForSet().add("rate:limit:whitelist", ip);
        assertThat(service.isBlacklisted(ip)).isFalse();
        redis.opsForSet().remove("rate:limit:whitelist", ip);
        assertThat(service.isBlacklisted(ip)).isTrue();
        redis.opsForSet().remove("rate:limit:blacklist", ip);
        assertThat(service.isBlacklisted(ip)).isFalse();

        String temporary = "rate:limit:blacklist:temp:" + ip;
        redis.opsForValue().set(temporary, "isolated test", 1, TimeUnit.MINUTES);
        assertThat(service.isBlacklisted(ip)).isTrue();
        redis.opsForSet().add("rate:limit:whitelist", ip);
        assertThat(service.isBlacklisted(ip)).isFalse();
        redis.opsForSet().remove("rate:limit:whitelist", ip);
        assertThat(service.isBlacklisted(ip)).isTrue();
        redis.delete(temporary);
        assertThat(service.isBlacklisted(ip)).isFalse();
    }
}
