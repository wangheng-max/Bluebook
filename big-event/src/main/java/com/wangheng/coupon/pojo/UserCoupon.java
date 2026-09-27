package com.wangheng.coupon.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * 用户优惠券实体（表 user_coupon）。
 * status: 0=未使用 1=已使用 2=已过期(含主动取消)。
 * uk_user_stock(user_id, coupon_stock_id) 保证同一活动每人只能持有一张。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserCoupon {
    private Integer id;
    private Integer userId;
    private Integer couponTypeId;
    private Integer couponStockId;
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private Integer useOrderId;
}
