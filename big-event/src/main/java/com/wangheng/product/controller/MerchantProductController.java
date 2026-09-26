package com.wangheng.product.controller;

import com.wangheng.anno.RequireMerchant;
import com.wangheng.common.PageBean;
import com.wangheng.product.pojo.Product;
import com.wangheng.common.Result;
import com.wangheng.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家商品管理接口（商城团购功能，仅认证商家）。
 * 供商家中心"商品管理"页使用：查看本店全部商品（含下架），按状态筛选。
 */
@RestController
@RequestMapping("/merchant/product")
public class MerchantProductController {

    @Autowired
    private ProductService productService;

    /** 本店商品列表（分页，status 可选筛选，缺省全部） */
    @RequireMerchant
    @GetMapping
    public Result<PageBean<Product>> myProducts(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) Integer status) {
        return Result.success(productService.myProducts(pageNum, pageSize, status));
    }
}
