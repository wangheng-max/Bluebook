package com.wangheng.product.controller;

import com.wangheng.anno.RequireAdmin;
import com.wangheng.anno.RequireMerchant;
import com.wangheng.article.pojo.CategorySaveDTO;
import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.product.pojo.Product;
import com.wangheng.product.pojo.ProductCategory;
import com.wangheng.product.pojo.ProductCategoryVO;
import com.wangheng.product.pojo.ProductSaveDTO;
import com.wangheng.product.pojo.ProductSku;
import com.wangheng.product.service.ProductCategoryService;
import com.wangheng.product.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商品接口（商城团购功能）。
 * 列表/详情/搜索/分类公开；发布/修改/下架仅认证商家；分类维护仅管理员。
 */
@RestController
@RequestMapping("/product")
public class ProductController {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductCategoryService productCategoryService;

    /** 商品列表（公开分页，status 缺省 1=上架） */
    @GetMapping
    public Result<PageBean<Product>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) Integer categoryId,
                                          @RequestParam(required = false) Integer status) {
        return Result.success(productService.pageList(pageNum, pageSize, categoryId, status));
    }

    /** 商品搜索（公开，仅上架） */
    @GetMapping("/search")
    public Result<PageBean<Product>> search(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) Integer categoryId,
                                            @RequestParam(required = false) BigDecimal minPrice,
                                            @RequestParam(required = false) BigDecimal maxPrice,
                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(productService.search(keyword, categoryId, minPrice, maxPrice, pageNum, pageSize));
    }

    /** 商品详情（公开；下架商品仅创建者可预览） */
    @GetMapping("/{id}")
    public Result<Product> detail(@PathVariable Integer id) {
        return Result.success(productService.detail(id));
    }

    /** 商品 SKU 列表（公开；详情页规格选择、商家编辑商品回显） */
    @GetMapping("/{id}/sku")
    public Result<List<ProductSku>> skuList(@PathVariable Integer id) {
        return Result.success(productService.listSku(id));
    }

    /** 发布商品（仅认证商家） */
    @RequireMerchant
    @PostMapping
    public Result publish(@RequestBody @Validated ProductSaveDTO dto) {
        productService.publish(dto);
        return Result.success();
    }

    /** 修改商品（仅认证商家，且只能改自己的） */
    @RequireMerchant
    @PutMapping
    public Result update(@RequestBody @Validated ProductSaveDTO dto) {
        productService.update(dto);
        return Result.success();
    }

    /** 下架商品（仅认证商家，且只能下架自己的） */
    @RequireMerchant
    @DeleteMapping("/{id}")
    public Result offShelf(@PathVariable Integer id) {
        productService.offShelf(id);
        return Result.success();
    }

    /** 分类平铺列表（公开） */
    @GetMapping("/category/list")
    public Result<List<ProductCategory>> categoryList() {
        return Result.success(productCategoryService.listFlat());
    }

    /** 分类树（公开，缓存 1 天） */
    @GetMapping("/category/tree")
    public Result<List<ProductCategoryVO>> categoryTree() {
        return Result.success(productCategoryService.tree());
    }

    /** 添加分类（仅管理员） */
    @RequireAdmin
    @PostMapping("/category")
    public Result addCategory(@RequestBody @Validated CategorySaveDTO dto) {
        productCategoryService.add(dto);
        return Result.success();
    }

    /** 修改分类（仅管理员） */
    @RequireAdmin
    @PutMapping("/category")
    public Result updateCategory(@RequestBody @Validated CategorySaveDTO dto) {
        productCategoryService.update(dto);
        return Result.success();
    }

    /** 删除分类（仅管理员） */
    @RequireAdmin
    @DeleteMapping("/category")
    public Result deleteCategory(@RequestParam Integer id) {
        productCategoryService.delete(id);
        return Result.success();
    }
}
