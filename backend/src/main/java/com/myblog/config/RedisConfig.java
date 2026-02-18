package com.myblog.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
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
     * 配置缓存管理器
     * 设计亮点：
     * - 设置默认过期时间为1小时（避免缓存永久存在）
     * - 禁用缓存null值（避免缓存穿透）
     * - 使用JSON序列化（便于调试）
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 使用GenericJackson2JsonRedisSerializer
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        
        // 配置缓存
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1)) // 默认过期时间1小时
                .disableCachingNullValues() // 不缓存null值
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .build();
    }
}
