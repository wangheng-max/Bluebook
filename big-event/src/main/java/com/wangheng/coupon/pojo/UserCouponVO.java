package com.wangheng.coupon.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 我的优惠券返回对象（GET /coupon/available、/coupon/my-list、/coupon/stock/my-records）：
 * 用户优惠券 + 券类型摘要。
 */
@Data
public class UserCouponVO {
    private Integer id;
    private Integer couponTypeId;
    private String typeName;
    private BigDecimal minSpend;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer validDays;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    /** 0=未使用 1=已使用 2=已过期 */
    private Integer status;
}
