package com.wangheng.order.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;


/**
 * 商家退款审核参数（POST /merchant/refund/{id}/audit）
 */
@Data
public class RefundAuditDTO {
    /** true=通过 false=拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    private String remark;
}
