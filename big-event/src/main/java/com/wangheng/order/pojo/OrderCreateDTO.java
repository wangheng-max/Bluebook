package com.wangheng.order.pojo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;









/**
 * 创建订单参数（POST /order）。
 * orderType=2（团购订单）时 groupBuyId 必传。
 */
@Data
public class OrderCreateDTO {
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;
    private Integer groupBuyId;
    @NotNull(message = "收货地址不能为空")
    private Integer addressId;
    /** 使用的用户优惠券ID（可选） */
    private Integer userCouponId;
    private String remark;
    @NotEmpty(message = "商品不能为空")
    @Valid
    private List<OrderItemDTO> productItems;
}
