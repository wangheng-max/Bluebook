package com.wangheng.coupon.controller;

import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.coupon.pojo.CouponGrabResult;
import com.wangheng.coupon.pojo.CouponStockVO;
import com.wangheng.coupon.pojo.UserCouponVO;
import com.wangheng.coupon.service.CouponGrabService;
import com.wangheng.coupon.service.CouponService;
import com.wangheng.coupon.util.CouponRateLimiter;



















import com.wangheng.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀抢券接口。
 * 抢购链路：限流 → 幂等占坑(SETNX) → Lua 原子扣减 → 落库（设计文档《秒杀抢券高并发方案》）。
 */
@RestController
@RequestMapping("/coupon/stock")
public class CouponStockController {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:coupon:grab:";

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponGrabService couponGrabService;

    @Autowired
    private CouponRateLimiter couponRateLimiter;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /** 抢购活动列表（公开分页，status 缺省 1=进行中） */
    @GetMapping("/list")
    public Result<PageBean<CouponStockVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(couponService.stockPageList(pageNum, pageSize, status));
    }

    /** 抢购活动详情（公开） */
    @GetMapping("/{id}")
    public Result<CouponStockVO> detail(@PathVariable Integer id) {
        return Result.success(couponService.stockDetail(id));
    }

    /** 我的抢购记录（分页） */
    @GetMapping("/my-records")
    public Result<PageBean<UserCouponVO>> myRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(couponService.myRecords(pageNum, pageSize));
    }

    /** 抢购优惠券（幂等 + 限流 + Lua 原子扣减） */
    @PostMapping("/{id}/grab")
    public Result<CouponGrabResult> grab(@PathVariable Integer id,
                                         @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        // 1. 限流：每分钟最多 10 次
        if (!couponRateLimiter.tryAcquire(userId)) {
            return Result.error("抢购过于频繁，请稍后再试");
        }

        // 2. 幂等占坑（SETNX）：抢占成功才执行，客户端缺省时服务端兜底生成
        if (!StringUtils.hasText(idempotencyKey)) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        String idempotentKey = IDEMPOTENT_KEY_PREFIX + idempotencyKey;
        try {
            Boolean grabbed = stringRedisTemplate.opsForValue().setIfAbsent(
                    idempotentKey, "granted", 1, TimeUnit.HOURS);
            if (!Boolean.TRUE.equals(grabbed)) {
                return Result.error("请勿重复提交抢购请求");
            }
        } catch (Exception e) {
            // Redis 不可用时跳过幂等（DB 唯一键仍兜底防重）
        }

        // 3. 执行抢购（本系统每人每次 1 张）
        return couponGrabService.grab(id, userId, 1);
    }

    /** 取消抢购：券作废，库存回补 */
    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Integer id) {
        couponService.cancelGrab(id);
        return Result.success();
    }
}
