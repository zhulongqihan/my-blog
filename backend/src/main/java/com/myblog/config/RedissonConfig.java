package com.myblog.config;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置
 * 
 * 提供 RedissonClient Bean，用于：
 * - RBloomFilter（缓存穿透防御）
 * - RLock（一人一赞分布式锁）
 * 
 * 与现有 Lettuce（StringRedisTemplate/RedisTemplate）共存，互不影响
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        String address = "redis://" + redisHost + ":" + redisPort;

        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(address)
                .setDatabase(0)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(8);

        if (StrUtil.isNotBlank(redisPassword)) {
            serverConfig.setPassword(redisPassword);
        }

        return Redisson.create(config);
    }
}
