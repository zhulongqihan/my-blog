package com.myblog.config;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/** Owns only the optional cache connection; the default factory remains Boot-managed. */
public final class CacheRedisConnection implements AutoCloseable {
    private final RedisConnectionFactory factory;
    private final boolean owned;

    CacheRedisConnection(RedisConnectionFactory factory, boolean owned) {
        this.factory = factory;
        this.owned = owned;
    }

    public RedisConnectionFactory factory() {
        return factory;
    }

    @Override
    public void close() {
        if (owned) {
            ((LettuceConnectionFactory) factory).destroy();
        }
    }
}
