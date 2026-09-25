package com.wangheng.pojo;

import lombok.Data;

/**
 * 商家优惠券统计（GET /merchant/coupon/statistics）
 */
@Data
public class CouponStatisticsVO {
    /** 券类型数 */
    private Integer typeCount;
    /** 发放活动数 */
    private Integer stockCount;
    /** 已发放（领取）张数 */
    private Integer grantedCount;
    /** 已使用张数 */
    private Integer usedCount;
}
