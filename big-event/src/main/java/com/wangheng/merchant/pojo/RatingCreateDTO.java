package com.wangheng.merchant.pojo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商家评分创建参数（POST /merchant/rating，订单维度一单一评）。
 */
@Data
public class RatingCreateDTO {
    @NotNull(message = "订单ID不能为空")
    private Integer orderId;
    @NotNull(message = "请打分")
    @Min(value = 1, message = "评分最低 1 星")
    @Max(value = 5, message = "评分最高 5 星")
    private Integer score;
    /** 评价内容，选填 */
    private String content;
}
