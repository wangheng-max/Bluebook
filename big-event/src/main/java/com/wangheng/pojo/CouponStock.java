package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 优惠券库存（发放活动）实体（表 coupon_stock）。
 * status: 0=未开始 1=进行中 2=已结束 3=已售罄。
 * 剩余数量以 Redis coupon:stock:{id} 为准，DB remain_count 由定时任务对账。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CouponStock {
    private Integer id;
    private Integer couponTypeId;
    private Integer totalCount;
    private Integer remainCount;
    private Integer usedCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    private Integer status;
    private Integer createUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
