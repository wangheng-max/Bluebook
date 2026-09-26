package com.wangheng.article.pojo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 商品分类添加/修改参数（POST/PUT /product/category，管理员接口）。
 * id 在修改时必传，添加时忽略。
 */
@Data
public class CategorySaveDTO {
    private Integer id;
    @NotEmpty(message = "分类名称不能为空")
    private String name;
    /** 父分类ID，缺省 0=顶级分类 */
    private Integer parentId;
    private String icon;
    private Integer sortOrder;
}
