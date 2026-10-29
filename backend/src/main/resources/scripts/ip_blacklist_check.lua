-- KEYS: whitelist SET, permanent blacklist SET, temporary blacklist key.
-- ARGV[1] uses RedisTemplate's configured value serializer, matching existing SET members.
-- Keep whitelist precedence and do not cache or suppress failures of this security decision.
if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
    return 0
end
if redis.call('SISMEMBER', KEYS[2], ARGV[1]) == 1 then
    return 1
end
return redis.call('EXISTS', KEYS[3])
