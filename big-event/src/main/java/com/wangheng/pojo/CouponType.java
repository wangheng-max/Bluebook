package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券类型实体（表 coupon_type，商家创建）。
 * 满减券用 min_spend + discount_amount；折扣券用 discount_rate（0.85=85折）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponType {
    private Integer id;
    private String name;
    private BigDecimal minSpend;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer giftProductId;
    private Integer maxUses;
    private Integer validDays;
    private Integer createUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
