package com.wangheng.pojo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品 SKU 发布/修改参数（ProductSaveDTO.skuList 的元素）
 */
@Data
public class ProductSkuDTO {
    @NotEmpty(message = "SKU 名称不能为空")
    private String skuName;
    /** 规格值列表，如 ["黑色","256G"] */
    private List<String> specValues;
    @NotNull(message = "SKU 价格不能为空")
    @DecimalMin(value = "0.01", message = "SKU 价格必须大于 0")
    private BigDecimal price;
    @Min(value = 0, message = "SKU 库存不能为负")
    private Integer stock;
}
