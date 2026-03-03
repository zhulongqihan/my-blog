package com.myblog.common.redis;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 逻辑过期包装类
 * 
 * 用于缓存击穿防御 — 逻辑过期方案。
 * 缓存中不设物理 TTL，而是在数据中携带 expireTime，
 * 读取时判断是否逻辑过期，异步重建。
 */
@Data
public class RedisData {
    /** 逻辑过期时间 */
    private LocalDateTime expireTime;
    /** 实际数据（JSON） */
    private Object data;
}
