package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付流水实体（表 payment_log）。
 * 本期为模拟支付：pay_type 0=未定 1=微信 2=支付宝；pay_status 0=待支付 1=已支付 2=失败。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLog {
    private Integer id;
    private Integer orderId;
    private Integer payType;
    private BigDecimal payAmount;
    private Integer payStatus;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;
    private String thirdPartyTradeNo;
    private String remark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
