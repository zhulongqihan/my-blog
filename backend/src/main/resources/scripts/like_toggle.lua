-- like_toggle.lua
-- 原子操作：判断 + 切换点赞状态 + 更新计数
--
-- KEYS[1] = article:liked:{articleId}  (SET)
-- KEYS[2] = article:like:count:{articleId}  (String)
-- ARGV[1] = userId

local isMember = redis.call('SISMEMBER', KEYS[1], ARGV[1])

if isMember == 1 then
    -- 已赞 → 取消：从 SET 移除 + 计数 -1
    redis.call('SREM', KEYS[1], ARGV[1])
    local count = redis.call('DECR', KEYS[2])
    -- 防止负数
    if count < 0 then
        redis.call('SET', KEYS[2], 0)
        count = 0
    end
    return {0, count}
else
    -- 未赞 → 点赞：加入 SET + 计数 +1
    redis.call('SADD', KEYS[1], ARGV[1])
    local count = redis.call('INCR', KEYS[2])
    return {1, count}
end
