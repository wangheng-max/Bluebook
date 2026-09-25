-- stock.lua
-- KEYS[1] = coupon:stock:{id}   ARGV[1] = userId   ARGV[2] = stockId   ARGV[3] = quantity
local key = KEYS[1]
local userId = ARGV[1]
local stockId = ARGV[2]
local quantity = tonumber(ARGV[3])

-- 防重：同一用户同一活动只能抢一次（Set 记录，与 Lua 外的防重合并为一步原子操作）
local recordKey = 'coupon:record:' .. userId
if redis.call('sismember', recordKey, stockId) == 1 then
    return -2  -- 已抢购过
end

-- 检查并原子扣减库存（注意 get 返回字符串，必须 tonumber 再比较）
local remain = tonumber(redis.call('get', key))
if remain == nil or remain < quantity then
    return -1  -- 库存不足
end

local stock = redis.call('decrby', key, quantity)

-- 记录抢购成功
redis.call('sadd', recordKey, stockId)

return stock  -- 返回剩余库存
