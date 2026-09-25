package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单返回对象：订单 + 明细列表 + 最新退款单状态（null=无退款申请）。
 */
@Data
public class OrderVO {
    private Integer id;
    private String orderNo;
    private Integer orderType;
    private Integer userId;
    private Integer groupBuyId;
    private Integer addressId;
    private BigDecimal totalAmount;
    private BigDecimal payAmount;
    private Integer userCouponId;
    /** 0=待支付 1=待发货 2=已发货 3=已完成 4=已取消 5=退款中 6=已退款 */
    private Integer status;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime shipTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime finishTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    /** 最新退款单状态（无退款申请为 null） */
    private Integer refundStatus;
    private List<OrderItem> items;
}
