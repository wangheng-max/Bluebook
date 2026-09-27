package com.wangheng.product.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



/**
 * 商品分类实体（表 product_category，管理员维护，商家仅可选用）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategory {
    private Integer id;
    private String name;
    private Integer parentId;
    private String icon;
    private Integer sortOrder;
    private Integer isSystem;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
