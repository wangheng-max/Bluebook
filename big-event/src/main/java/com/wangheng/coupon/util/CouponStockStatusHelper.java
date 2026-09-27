package com.wangheng.coupon.util;

import com.wangheng.coupon.mapper.CouponStockMapper;
import com.wangheng.coupon.pojo.CouponStock;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;












/**
 * 优惠券发放活动状态工具（抢券/展示/商家管理共用）：
 * 状态懒刷新（0未开始→1进行中→2已结束）+ Redis 实时剩余数量。
 */
@Component
public class CouponStockStatusHelper {

    /** 发放活动剩余库存 Redis key 前缀（Redis 是扣减源，DB 由对账任务对齐） */
    public static final String STOCK_KEY_PREFIX = "coupon:stock:";

    @Autowired
    private CouponStockMapper couponStockMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 状态懒刷新：0未开始→1进行中；进行中到期→2已结束（按需回写 DB） */
    public void refreshStatus(CouponStock stock) {
        LocalDateTime now = LocalDateTime.now();
        if (stock.getStatus() != null && stock.getStatus() == 0 && !now.isBefore(stock.getStartTime())) {
            stock.setStatus(1);
            couponStockMapper.updateStatus(stock.getId(), 1);
        }
        if (stock.getStatus() != null && stock.getStatus() == 1 && now.isAfter(stock.getEndTime())) {
            stock.setStatus(2);
            couponStockMapper.updateStatus(stock.getId(), 2);
        }
    }

    /** 实时剩余数量：Redis 优先，缺失回 DB */
    public int liveRemain(Integer stockId, Integer dbRemain) {
        try {
            String v = stringRedisTemplate.opsForValue().get(STOCK_KEY_PREFIX + stockId);
            if (v != null) {
                return Integer.parseInt(v);
            }
        } catch (Exception ignored) {
        }
        return dbRemain == null ? 0 : dbRemain;
    }
}
