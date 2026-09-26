package com.wangheng.order.pojo;







import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单商品项参数（OrderCreateDTO.productItems 的元素）
 */
@Data
public class OrderItemDTO {
    @NotNull(message = "商品不能为空")
    private Integer productId;
    /** SKU ID（可选，商品有规格时传） */
    private Integer skuId;
    @NotNull(message = "购买数量不能为空")
    @Min(value = 1, message = "购买数量至少为 1")
    private Integer quantity;
}
