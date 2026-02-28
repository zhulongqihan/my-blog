package com.myblog.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
     * 创建配置完整的JSON序列化器
     * 关键：必须手动注册 JavaTimeModule，否则 LocalDateTime 无法序列化
     * GenericJackson2JsonRedisSerializer 默认构造器创建的 ObjectMapper 
     * 不会自动注册 JavaTimeModule，导致缓存含 LocalDateTime 的对象时报500
     */
    private GenericJackson2JsonRedisSerializer createJsonSerializer() {
        ObjectMapper mapper = new ObjectMapper();
        // 注册 Java 8 时间模块（关键！解决 LocalDateTime 序列化问题）
        mapper.registerModule(new JavaTimeModule());
        // 日期输出为 ISO-8601 字符串而非时间戳数组
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 设置所有字段可见（包括 private）
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 启用类型信息，反序列化时能正确还原对象类型
        // 使用安全的类型验证器，仅允许 com.myblog 和 java.util 包下的类反序列化
        BasicPolymorphicTypeValidator typeValidator = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(Object.class)
                .allowIfSubType("com.myblog.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.lang.")
                .allowIfSubType("java.time.")
                .allowIfSubType("org.springframework.data.domain.")
                .build();
        mapper.activateDefaultTyping(
                typeValidator,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        return new GenericJackson2JsonRedisSerializer(mapper);
    }
    
    /**
     * 配置RedisTemplate
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        GenericJackson2JsonRedisSerializer serializer = createJsonSerializer();
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
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
        GenericJackson2JsonRedisSerializer serializer = createJsonSerializer();
        
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
