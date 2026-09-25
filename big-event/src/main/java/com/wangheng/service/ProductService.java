package com.wangheng.service;

import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Product;
import com.wangheng.pojo.ProductSaveDTO;
import com.wangheng.pojo.ProductSku;

import java.math.BigDecimal;
import java.util.List;

/**
 * 商品服务。
 * 列表/详情/搜索公开；发布/修改/下架仅认证商家（@RequireMerchant + 归属校验）。
 */
public interface ProductService {

    /** 商品列表（公开分页；status 缺省 1=上架） */
    PageBean<Product> pageList(Integer pageNum, Integer pageSize, Integer categoryId, Integer status);

    /** 商品搜索（公开，仅上架；关键词/分类/价格区间） */
    PageBean<Product> search(String keyword, Integer categoryId,
                             BigDecimal minPrice, BigDecimal maxPrice,
                             Integer pageNum, Integer pageSize);

    /** 商品详情（公开；下架商品仅创建者可预览） */
    Product detail(Integer id);

    /** 商品 SKU 列表（公开；详情页规格选择、商家编辑回显用） */
    List<ProductSku> listSku(Integer productId);

    /** 发布商品（仅认证商家） */
    void publish(ProductSaveDTO dto);

    /** 修改商品（仅认证商家，且只能改自己的） */
    void update(ProductSaveDTO dto);

    /** 下架商品（仅认证商家，且只能下架自己的） */
    void offShelf(Integer id);

    /** 商家自己的商品列表（数据隔离） */
    PageBean<Product> myProducts(Integer pageNum, Integer pageSize, Integer status);
}
