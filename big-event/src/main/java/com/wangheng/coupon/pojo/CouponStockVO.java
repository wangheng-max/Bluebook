package com.wangheng.coupon.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 抢购活动返回对象（GET /coupon/stock/list、GET /coupon/stock/{id}）：
 * 库存活动 + 券类型摘要 + 实时剩余数量 + 我的抢购状态。
 */
@Data
public class CouponStockVO {
    private Integer id;
    private Integer couponTypeId;
    private String typeName;
    private BigDecimal minSpend;
    private BigDecimal discountAmount;
    private BigDecimal discountRate;
    private Integer validDays;
    private Integer totalCount;
    private Integer remainCount;
    private Integer usedCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    /** 0=未开始 1=进行中 2=已结束 3=已售罄 */
    private Integer status;
    /** 我是否已抢过（登录态返回，未登录恒 false） */
    private Boolean grabbed;
}
