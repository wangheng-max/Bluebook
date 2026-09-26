package com.wangheng.order.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 支付参数（POST /order/{id}/pay，本期模拟支付）
 */
@Data
public class PayDTO {
    @NotNull(message = "支付方式不能为空")
    @Min(value = 1, message = "支付方式不正确")
    @Max(value = 2, message = "支付方式不正确")
    private Integer payType;
}
