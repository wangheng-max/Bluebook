package com.wangheng.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 定向发放参数（POST /merchant/coupon/{stockId}/grant，商家接口）。
 * uk_user_stock 约束下同一用户同一活动只能持有一张。
 */
@Data
public class GrantDTO {
    @NotNull(message = "目标用户不能为空")
    private Integer userId;
}
