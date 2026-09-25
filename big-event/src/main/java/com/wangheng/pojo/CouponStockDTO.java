package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 优惠券发放活动创建参数（POST /merchant/coupon/stock，商家接口）
 */
@Data
public class CouponStockDTO {
    @NotNull(message = "优惠券类型不能为空")
    private Integer couponTypeId;
    @NotNull(message = "发放总量不能为空")
    @Min(value = 1, message = "发放总量至少为 1")
    private Integer totalCount;
    @NotNull(message = "抢购开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "抢购结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
