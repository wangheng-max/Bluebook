package com.wangheng.coupon.util;

import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;



/**
 * 抢券限流器：每个用户每分钟最多 10 次抢购请求（设计文档《瞬时流量控制》）。
 * Redis 计数窗口实现；Redis 不可用时放行（学习项目，保证可用性优先）。
 */
@Component
public class CouponRateLimiter {

    private static final String KEY_PREFIX = "rate_limit:coupon:";
    private static final int MAX_PER_MINUTE = 10;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 尝试获取一次抢购配额
     * @return true=放行 false=超限
     */
    public boolean tryAcquire(Integer userId) {
        try {
            String key = KEY_PREFIX + userId;
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                // 首次访问设置 1 分钟窗口
                stringRedisTemplate.expire(key, 1, TimeUnit.MINUTES);
            }
            return count == null || count <= MAX_PER_MINUTE;
        } catch (Exception e) {
            return true;
        }
    }
}
