package com.wangheng.product.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 带货商品简要信息（文章卡片/详情展示，点击跳商品页）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductBriefVO {
    private Integer id;
    private String name;
    private String coverImg;
    private BigDecimal price;
    /** 商品是否在架（下架商品前端置灰不可点） */
    private Integer status;
}
