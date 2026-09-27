package com.wangheng.order.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 退款单返回对象：退款单 + 订单摘要。
 */
@Data
public class RefundOrderVO {
    private Integer id;
    private Integer orderId;
    private String orderNo;
    private Integer orderType;
    /** 订单实付金额 */
    private BigDecimal payAmount;
    private BigDecimal refundAmount;
    private String refundReason;
    /** 0=待处理 1=已通过 2=已拒绝 3=已退款 */
    private Integer refundStatus;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
