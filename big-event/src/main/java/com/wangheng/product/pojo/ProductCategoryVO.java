package com.wangheng.product.pojo;

import java.util.List;
import lombok.Data;



/**
 * 商品分类树节点（GET /product/category/tree）
 */
@Data
public class ProductCategoryVO {
    private Integer id;
    private String name;
    private Integer parentId;
    private String icon;
    private Integer sortOrder;
    private Integer isSystem;
    private List<ProductCategoryVO> children;
}
