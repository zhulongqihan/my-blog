package com.myblog.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解
 * 
 * 设计目标：通过注解灵活配置不同接口的限流策略，基于Redis + Lua脚本实现分布式限流
 * 
 * 限流算法：滑动窗口（Sliding Window）
 * - 比固定窗口更精确，避免窗口边界的突发流量问题
 * - 基于Redis的ZSET实现，用时间戳作为score
 * 
 * 限流维度：
 * - IP: 基于客户端IP限流（默认）
 * - USER: 基于已认证用户ID限流
 * - IP_AND_API: 基于IP + 接口路径限流（更精细）
 * 
 * 使用示例：
 * // 默认：60秒内最多30次请求
 * @RateLimit
 * 
 * // 自定义：10秒内最多5次请求（适用于登录接口）
 * @RateLimit(maxRequests = 5, timeWindow = 10, timeUnit = TimeUnit.SECONDS, message = "登录尝试过于频繁")
 * 
 * // 基于用户限流：1分钟内最多10次请求
 * @RateLimit(maxRequests = 10, limitType = LimitType.USER)
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 最大请求次数
     * 在timeWindow时间窗口内允许的最大请求数
     */
    int maxRequests() default 30;
    
    /**
     * 时间窗口大小
     */
    int timeWindow() default 60;
    
    /**
     * 时间窗口单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    
    /**
     * 限流维度
     */
    LimitType limitType() default LimitType.IP;
    
    /**
     * 限流key前缀（可选，默认使用方法签名）
     * 用于自定义分组限流，如登录接口统一使用 "login" 前缀
     */
    String prefix() default "";
    
    /**
     * 被限流时的提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
    
    /**
     * 限流维度枚举
     */
    enum LimitType {
        /** 基于IP限流 */
        IP,
        /** 基于用户ID限流（需要已认证） */
        USER,
        /** 基于IP + 接口路径限流 */
        IP_AND_API
    }
}
