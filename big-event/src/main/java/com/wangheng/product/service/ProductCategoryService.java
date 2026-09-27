package com.wangheng.product.service;

import com.wangheng.article.pojo.CategorySaveDTO;
import com.wangheng.product.pojo.ProductCategory;
import com.wangheng.product.pojo.ProductCategoryVO;
import java.util.List;

/**
 * 商品分类服务（管理员维护，商家/用户只读）
 */
public interface ProductCategoryService {

    /** 分类树（含子分类嵌套，缓存 1 天） */
    List<ProductCategoryVO> tree();

    /** 分类平铺列表 */
    List<ProductCategory> listFlat();

    /** 添加分类（仅管理员，@RequireAdmin 保护） */
    void add(CategorySaveDTO dto);

    /** 修改分类（仅管理员） */
    void update(CategorySaveDTO dto);

    /** 删除分类（仅管理员；有子分类或商品时拒绝） */
    void delete(Integer id);
}
