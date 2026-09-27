package com.wangheng.product.pojo;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * 商品 SKU 实体（表 product_sku，支持规格与独立库存）。
 * specValues 为 JSON 数组字符串。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductSku {
    private Integer id;
    private Integer productId;
    private String skuName;
    /** 规格值 JSON 数组字符串，如 ["黑色","256G"] */
    private String specValues;
    private BigDecimal price;
    private Integer stock;
    private Integer salesCount;
}
