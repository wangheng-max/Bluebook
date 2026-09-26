package com.wangheng.coupon.pojo;

import lombok.Data;

/**
 * 抢购结果（POST /coupon/stock/{id}/grab）
 */
@Data
public class CouponGrabResult {
    private Integer couponStockId;
    private Integer quantity;
    private Integer remainStock;
    private Integer userCouponId;
}
