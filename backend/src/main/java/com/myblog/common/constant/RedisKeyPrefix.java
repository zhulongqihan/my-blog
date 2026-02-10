package com.myblog.common.constant;

/**
 * Redis Key前缀常量
 * 设计目标：统一管理Redis Key，避免Key冲突，便于维护
 * 
 * 命名规范：模块:功能:具体标识
 * 示例：jwt:blacklist:token123
 */
public class RedisKeyPrefix {
    
    /**
     * JWT黑名单前缀
     * 用途：存储已登出的Token，防止Token被重复使用
     * Key格式：jwt:blacklist:{token}
     * Value：用户ID
     * TTL：Token剩余有效期
     */
    public static final String JWT_BLACKLIST = "jwt:blacklist:";
    
    /**
     * 文章缓存前缀
     * Key格式：article:detail:{articleId}
     * Value：文章详情对象（JSON）
     * TTL：1小时
     */
    public static final String ARTICLE_DETAIL = "article:detail:";
    
    /**
     * 文章列表缓存前缀
     * Key格式：article:list:{page}:{size}:{categoryId}
     * Value：文章列表（JSON）
     * TTL：10分钟
     */
    public static final String ARTICLE_LIST = "article:list:";
    
    /**
     * 热门文章缓存
     * Key格式：article:hot
     * Value：文章ID列表（ZSet，按浏览量排序）
     * TTL：1小时
     */
    public static final String ARTICLE_HOT = "article:hot";
    
    /**
     * 文章浏览量计数器
     * Key格式：article:view:count:{articleId}
     * Value：浏览量（String）
     * TTL：永久（定期同步到数据库）
     */
    public static final String ARTICLE_VIEW_COUNT = "article:view:count:";
    
    /**
     * 分类缓存前缀
     * Key格式：category:list
     * Value：分类列表（JSON）
     * TTL：1小时
     */
    public static final String CATEGORY_LIST = "category:list";
    
    /**
     * 标签缓存前缀
     * Key格式：tag:list
     * Value：标签列表（JSON）
     * TTL：1小时
     */
    public static final String TAG_LIST = "tag:list";
    
    /**
     * 用户信息缓存
     * Key格式：user:info:{userId}
     * Value：用户信息（JSON）
     * TTL：30分钟
     */
    public static final String USER_INFO = "user:info:";
    
    /**
     * 限流Key前缀
     * Key格式：rate:limit:{ip}:{api}
     * Value：访问次数（String）
     * TTL：根据限流策略设置
     */
    public static final String RATE_LIMIT = "rate:limit:";
}
