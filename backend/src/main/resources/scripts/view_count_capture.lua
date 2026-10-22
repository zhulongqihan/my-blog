-- Keep the previous batch until the database transaction has durably acknowledged it.
if redis.call('EXISTS', KEYS[2]) == 1 then
 return {redis.call('HGET', KEYS[2], 'batchId'), redis.call('HGET', KEYS[2], 'delta')}
end
local raw = redis.call('GET', KEYS[1])
if not raw then return {} end
local delta = tonumber(raw)
if not delta or delta <= 0 or delta ~= math.floor(delta) then
 return redis.error_reply('Invalid view-count increment')
end
redis.call('HSET', KEYS[2], 'batchId', ARGV[1], 'delta', raw)
redis.call('DEL', KEYS[1])
return {ARGV[1], raw}

