package com.wangheng.order.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款单实体（表 refund_order）。
 * refund_status: 0=待处理 1=已通过 2=已拒绝 3=已退款。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefundOrder {
    private Integer id;
    private Integer orderId;
    private BigDecimal refundAmount;
    private String refundReason;
    private Integer refundStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
