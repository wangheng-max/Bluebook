package com.wangheng.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细实体（表 order_item）。
 * 商品名/图/SKU 为下单时快照，商品后续修改不影响历史订单。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {
    private Integer id;
    private Integer orderId;
    private Integer productId;
    private String productName;
    private Integer skuId;
    private String skuName;
    private String productImg;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
    private BigDecimal couponDiscount;
}
