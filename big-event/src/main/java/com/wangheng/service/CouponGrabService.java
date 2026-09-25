package com.wangheng.service;

import com.wangheng.pojo.CouponGrabResult;
import com.wangheng.pojo.Result;

/**
 * 抢券服务（秒杀抢券核心，设计文档《秒杀抢券高并发方案》）。
 * Redis Lua 原子扣减 + 唯一索引兜底 + 幂等占坑（Controller 层）。
 */
public interface CouponGrabService {

    /**
     * 抢购优惠券
     * @param couponStockId 发放活动ID
     * @param userId        当前用户
     * @param quantity      抢购数量（本系统固定 1）
     */
    Result<CouponGrabResult> grab(Integer couponStockId, Integer userId, Integer quantity);
}
