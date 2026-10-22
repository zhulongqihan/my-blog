package com.myblog.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RedisIsolationConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
            .withUserConfiguration(RedisConfig.class);

    @Test
    void isolatedCacheDoesNotReplaceBootBusinessFactoryOrPrimaryTemplates() {
        runner.withPropertyValues("blog.cache.redis.enabled=true", "blog.cache.redis.port=16380")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(RedisConnectionFactory.class);
                    RedisConnectionFactory business = context.getBean(RedisConnectionFactory.class);
                    StringRedisTemplate state = context.getBean(StringRedisTemplate.class);
                    StringRedisTemplate cache = context.getBean("cacheStringRedisTemplate", StringRedisTemplate.class);
                    assertThat(state).isSameAs(context.getBean("stringRedisTemplate"));
                    assertThat(state.getConnectionFactory()).isSameAs(business);
                    assertThat(cache.getConnectionFactory()).isNotSameAs(business);
                    assertThat(context.getBean("cacheRedisTemplate", RedisTemplate.class).getConnectionFactory())
                            .isSameAs(cache.getConnectionFactory());
                    assertThat(context.getBean("redisTemplate", RedisTemplate.class).getConnectionFactory())
                            .isSameAs(business);
                });
    }

    @Test
    void localDefaultRetainsExistingSingleRedisSetup() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(RedisConnectionFactory.class);
            assertThat(context.getBean("cacheStringRedisTemplate", StringRedisTemplate.class).getConnectionFactory())
                    .isSameAs(context.getBean(RedisConnectionFactory.class));
        });
    }
}
