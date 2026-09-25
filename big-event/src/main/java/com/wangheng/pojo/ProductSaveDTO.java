package com.wangheng.pojo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品发布/修改参数（POST /product、PUT /product，商家接口）。
 * id 在修改时必传，发布时忽略。
 */
@Data
public class ProductSaveDTO {
    private Integer id;
    @NotEmpty(message = "商品名称不能为空")
    private String name;
    @NotNull(message = "商品分类不能为空")
    private Integer categoryId;
    @NotEmpty(message = "封面图不能为空")
    private String coverImg;
    private List<String> images;
    private String videoUrl;
    private String description;
    @NotNull(message = "商品价格不能为空")
    @DecimalMin(value = "0.01", message = "商品价格必须大于 0")
    private BigDecimal price;
    private BigDecimal marketPrice;
    @Min(value = 0, message = "库存不能为负")
    private Integer stock;
    /** 1=上架 0=下架，缺省 1 */
    private Integer status;
    private List<ProductSkuDTO> skuList;
}
