package com.wangheng.task;

import com.wangheng.coupon.mapper.UserCouponMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;








/**
 * 优惠券过期检查（设计文档《定时任务设计》）：每小时批量标记过期券。
 * 读接口对"可用券"另做过期过滤，本任务保证"我的优惠券"列表状态最终一致。
 */
@Slf4j
@Component
public class CouponExpireTask {

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void checkExpiredCoupons() {
        try {
            int count = userCouponMapper.markExpired();
            if (count > 0) {
                log.info("优惠券过期处理完成: {} 张", count);
            }
        } catch (Exception e) {
            log.error("优惠券过期检查失败", e);
        }
    }
}
