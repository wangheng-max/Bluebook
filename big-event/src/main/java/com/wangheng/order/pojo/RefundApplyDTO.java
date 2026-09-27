package com.wangheng.order.pojo;

import lombok.Data;


/**
 * 退款申请参数（POST /order/{id}/refund）
 */
@Data
public class RefundApplyDTO {
    private String refundReason;
}
