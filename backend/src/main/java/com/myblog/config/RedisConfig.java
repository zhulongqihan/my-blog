package com.myblog.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis配置类（Spring Boot 3.x 兼容版本）
 * 设计目标：
 * 1. 配置RedisTemplate，使用JSON序列化（可读性好，便于调试）
 * 2. 配置Spring Cache，支持@Cacheable注解
 * 3. 设置合理的缓存过期时间
 * 
 * 技术说明：
 * - Spring Boot 3.x中使用GenericJackson2JsonRedisSerializer替代Jackson2JsonRedisSerializer
 * - 避免使用过时的setObjectMapper方法
 */
@Configuration
@EnableCaching
public class RedisConfig {
    
    /**
     * 配置RedisTemplate
     * 设计亮点：
     * - Key使用String序列化（便于在Redis客户端查看）
     * - Value使用JSON序列化（可读性好，支持复杂对象）
     * - 自动处理类型信息（反序列化时能正确还原对象类型）
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用GenericJackson2JsonRedisSerializer（Spring Boot 3.x推荐）
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        
        // String序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        // Key使用String序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value使用JSON序列化
        template.setValueSerializer(serializer);
        template.setHashValueSerializer(serializer);
        
        template.afterPropertiesSet();
        return template;
    }
    
    /**
     * 配置缓存管理器（多级TTL）
     * 设计亮点：
     * - 不同缓存空间设置不同的过期时间（精细化控制）
     * - 高频变更数据短TTL，低频变更数据长TTL
     * - 禁用缓存null值（避免缓存穿透）
     * - 使用JSON序列化（便于调试）
     *
     * 缓存空间说明：
     *   articleDetail     - 文章详情（30分钟）
     *   featuredArticles  - 精选文章（10分钟）
     *   popularArticles   - 热门文章（10分钟）
     *   categories        - 分类列表（2小时）
     *   tags              - 标签列表（2小时）
     *   dashboardStats    - 仪表盘统计（5分钟）
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 使用GenericJackson2JsonRedisSerializer
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        
        // 默认缓存配置（1小时兜底）
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        
        // 各缓存空间的自定义TTL配置
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("articleDetail", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigs.put("featuredArticles", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("popularArticles", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigs.put("categories", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("tags", defaultConfig.entryTtl(Duration.ofHours(2)));
        cacheConfigs.put("dashboardStats", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}
