package com.wangheng.task;

import com.wangheng.mapper.CouponStockMapper;
import com.wangheng.pojo.CouponStock;
import com.wangheng.service.impl.CouponGrabServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 库存对账（设计文档《数据库库存同步（定时对账）》）：每 5 分钟将数据库库存与 Redis 对齐。
 * Redis 是扣减源，DB remain_count 以 Redis 为准；剩余为 0 时标记售罄。
 */
@Slf4j
@Component
public class CouponStockSyncTask {

    @Autowired
    private CouponStockMapper couponStockMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Scheduled(fixedDelay = 5 * 60 * 1000)
    public void syncStockToDatabase() {
        Set<String> keys = stringRedisTemplate.keys(CouponGrabServiceImpl.STOCK_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            try {
                // 从 key 解析 stockId，从 value 读取 Redis 剩余库存（库存数是 value，不是 key 的一部分）
                Integer stockId = Integer.parseInt(key.substring(key.lastIndexOf(":") + 1));
                String stockStr = stringRedisTemplate.opsForValue().get(key);
                if (stockStr == null) {
                    continue;
                }
                Long remain = Long.parseLong(stockStr);

                CouponStock dbStock = couponStockMapper.findById(stockId);
                if (dbStock == null) {
                    continue;
                }
                if (dbStock.getRemainCount() == null || dbStock.getRemainCount() != remain.intValue()) {
                    couponStockMapper.updateRemainCount(stockId, remain.intValue());
                    log.info("库存同步: stockId={}, remain={}", stockId, remain);
                    if (remain == 0 && dbStock.getStatus() != null && dbStock.getStatus() != 3) {
                        couponStockMapper.updateStatus(stockId, 3);  // 售罄
                    }
                }
            } catch (Exception e) {
                log.error("库存同步失败: {}", key, e);
            }
        }
    }
}
