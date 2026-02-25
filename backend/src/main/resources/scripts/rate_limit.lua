-- 滑动窗口限流 Lua 脚本
-- 原子操作：保证在高并发场景下的计数精确性
--
-- 原理：使用Redis ZSET（有序集合）实现滑动窗口
-- - 每个请求以当前时间戳作为score和member存入ZSET
-- - 清除窗口外的过期请求
-- - 统计窗口内的请求数量
-- - 如果未超限则允许通过，否则拒绝
--
-- KEYS[1]: 限流的Key（如 rate:limit:192.168.1.1:/api/articles）
-- ARGV[1]: 当前时间戳（毫秒）
-- ARGV[2]: 窗口起始时间戳（当前时间 - 窗口大小）
-- ARGV[3]: 最大请求次数
-- ARGV[4]: 窗口过期时间（秒，用于Key自动清理）
--
-- 返回值：
-- [0]: 0=允许通过, 1=被限流
-- [1]: 当前窗口内的请求数
-- [2]: 剩余允许的请求数

local key = KEYS[1]
local now = tonumber(ARGV[1])
local windowStart = tonumber(ARGV[2])
local maxRequests = tonumber(ARGV[3])
local expireTime = tonumber(ARGV[4])

-- Step 1: 清除窗口外的过期请求记录
redis.call('ZREMRANGEBYSCORE', key, 0, windowStart)

-- Step 2: 获取当前窗口内的请求数
local currentCount = redis.call('ZCARD', key)

-- Step 3: 判断是否超过限流阈值
if currentCount >= maxRequests then
    -- 被限流：返回限流标志、当前数量、剩余0
    return {1, currentCount, 0}
end

-- Step 4: 未超限，添加当前请求记录
-- 使用 时间戳+随机数 作为member避免重复
redis.call('ZADD', key, now, now .. '-' .. math.random(1000000))

-- Step 5: 设置Key的过期时间（防止废弃Key占用内存）
redis.call('EXPIRE', key, expireTime)

-- Step 6: 返回通过标志、当前数量+1、剩余次数
return {0, currentCount + 1, maxRequests - currentCount - 1}
