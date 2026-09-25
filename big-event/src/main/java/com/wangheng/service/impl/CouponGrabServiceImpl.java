package com.wangheng.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.mapper.CouponStockMapper;
import com.wangheng.mapper.CouponTypeMapper;
import com.wangheng.mapper.UserCouponMapper;
import com.wangheng.pojo.CouponGrabResult;
import com.wangheng.pojo.CouponStock;
import com.wangheng.pojo.CouponType;
import com.wangheng.pojo.Result;
import com.wangheng.pojo.UserCoupon;
import com.wangheng.service.CouponGrabService;
import com.wangheng.utils.CouponStockStatusHelper;
import com.wangheng.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 抢券服务实现。
 * 流程：校验活动状态 → 确保 Redis 库存已初始化 → Lua 原子扣减（防重 + 库存）→
 * 落库 user_coupon（uk_user_stock 兜底防并发重复）→ 失效缓存 + WebSocket 通知。
 */
@Service
public class CouponGrabServiceImpl implements CouponGrabService {

    /** 发放活动剩余库存 Redis key 前缀（与 CouponStockStatusHelper 同源，供对账任务/其他服务引用） */
    public static final String STOCK_KEY_PREFIX = CouponStockStatusHelper.STOCK_KEY_PREFIX;
    private static final String RECORD_KEY_PREFIX = "coupon:record:";
    private static final String USER_COUPON_CACHE_PREFIX = "coupon:user:available:";
    private static final String STOCK_INFO_CACHE_PREFIX = "coupon:stock:info:";

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private CouponStockMapper couponStockMapper;

    @Autowired
    private CouponTypeMapper couponTypeMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponStockStatusHelper couponStockStatusHelper;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<CouponGrabResult> grab(Integer couponStockId, Integer userId, Integer quantity) {
        // 1. 校验活动状态（懒刷新：0→1、1→2）
        CouponStock stock = couponStockMapper.findById(couponStockId);
        if (stock == null) {
            return Result.error("抢购活动不存在");
        }
        couponStockStatusHelper.refreshStatus(stock);
        if (stock.getStatus() != 1) {
            return Result.error("抢购活动未开始或已结束");
        }

        // 2. 确保 Redis 库存已初始化（首次抢购或缓存丢失时从 DB 恢复）
        String stockKey = STOCK_KEY_PREFIX + couponStockId;
        ensureStockKey(stockKey, stock);

        // 3. Lua 原子操作：防重复 + 扣减库存
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setLocation(new ClassPathResource("lua/stock.lua"));
        redisScript.setResultType(Long.class);
        String[] args = {String.valueOf(userId), String.valueOf(couponStockId), String.valueOf(quantity)};
        Long remain = stringRedisTemplate.execute(redisScript, List.of(stockKey), args);

        if (remain != null && remain == -2) {
            return Result.error("您已抢购过该优惠券");
        }
        if (remain == null || remain < 0) {
            // 库存不足：真实剩余为 0 时标记售罄
            if (remain != null && remain == -1) {
                try {
                    String v = stringRedisTemplate.opsForValue().get(stockKey);
                    if (v != null && Long.parseLong(v) <= 0) {
                        couponStockMapper.updateStatus(couponStockId, 3);
                        evictStockInfoCache(couponStockId);
                    }
                } catch (Exception ignored) {
                }
            }
            return Result.error("优惠券抢购失败，库存不足");
        }

        // 4. 落库用户优惠券（uk_user_stock 唯一键兜底并发重复）
        CouponType type = couponTypeMapper.findById(stock.getCouponTypeId());
        UserCoupon uc = new UserCoupon();
        uc.setUserId(userId);
        uc.setCouponTypeId(stock.getCouponTypeId());
        uc.setCouponStockId(couponStockId);
        uc.setStatus(0);
        uc.setStartTime(LocalDateTime.now());
        int validDays = type == null || type.getValidDays() == null ? 7 : type.getValidDays();
        uc.setEndTime(LocalDateTime.now().plusDays(validDays));
        try {
            userCouponMapper.insert(uc);
        } catch (DuplicateKeyException e) {
            // 并发下唯一键冲突：回补 Redis 库存（对账任务最终一致）
            try {
                stringRedisTemplate.opsForValue().increment(stockKey, quantity);
            } catch (Exception ignored) {
            }
            return Result.error("您已抢购过该优惠券");
        }

        // 5. 已使用数量 +1，失效用户可用券缓存，WebSocket 通知
        couponStockMapper.incrUsedCount(couponStockId);
        evictUserCouponCache(userId);
        notifyGrabSuccess(userId, couponStockId, uc.getId(), quantity, remain.intValue());

        CouponGrabResult result = new CouponGrabResult();
        result.setCouponStockId(couponStockId);
        result.setQuantity(quantity);
        result.setRemainStock(remain.intValue());
        result.setUserCouponId(uc.getId());
        return Result.success(result);
    }

    /** Redis 库存初始化：setIfAbsent 防并发重复初始化 */
    private void ensureStockKey(String stockKey, CouponStock stock) {
        try {
            stringRedisTemplate.opsForValue().setIfAbsent(
                    stockKey, String.valueOf(stock.getRemainCount() == null ? 0 : stock.getRemainCount()),
                    Duration.between(LocalDateTime.now(), stock.getEndTime()).plusHours(1));
        } catch (Exception ignored) {
        }
    }

    private void evictUserCouponCache(Integer userId) {
        try {
            stringRedisTemplate.delete(USER_COUPON_CACHE_PREFIX + userId);
        } catch (Exception ignored) {
        }
    }

    private void evictStockInfoCache(Integer stockId) {
        try {
            stringRedisTemplate.delete(STOCK_INFO_CACHE_PREFIX + stockId);
        } catch (Exception ignored) {
        }
    }

    /** 抢购成功 WebSocket 通知（复用现有聊天通道，离线用户看"我的抢购记录"） */
    private void notifyGrabSuccess(Integer userId, Integer stockId, Integer userCouponId,
                                   Integer quantity, Integer remainStock) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "COUPON_GRAB_SUCCESS");
            message.put("couponStockId", stockId);
            message.put("userCouponId", userCouponId);
            message.put("quantity", quantity);
            message.put("remainStock", remainStock);
            message.put("message", "恭喜您抢购成功！优惠券已放入您的账户，请及时使用");
            chatWebSocketHandler.sendToUser(userId, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 通知失败不影响抢购主流程
        }
    }
}
