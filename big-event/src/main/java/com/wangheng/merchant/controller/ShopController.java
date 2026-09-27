package com.wangheng.merchant.controller;

import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.merchant.pojo.MerchantRatingVO;
import com.wangheng.merchant.pojo.ShopVO;
import com.wangheng.merchant.service.ShopService;
import com.wangheng.product.pojo.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


/**
 * 店铺主页公开接口（匿名可访问，白名单见 LoginInterceptor ^/shop/\d+...）。
 * 就像淘宝/京东的店铺页：店铺信息 + 全部在架商品 + 买家评价。
 */
@RestController
@RequestMapping("/shop")
public class ShopController {

    @Autowired
    private ShopService shopService;

    /** 店铺信息 + 评分汇总 */
    @GetMapping("/{merchantUserId}")
    public Result<ShopVO> shop(@PathVariable Integer merchantUserId) {
        return Result.success(shopService.getShop(merchantUserId));
    }

    /** 店铺全部在架商品（分页） */
    @GetMapping("/{merchantUserId}/products")
    public Result<PageBean<Product>> products(@PathVariable Integer merchantUserId,
                                              @RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "12") Integer pageSize) {
        return Result.success(shopService.getShopProducts(merchantUserId, pageNum, pageSize));
    }

    /** 店铺买家评价（分页） */
    @GetMapping("/{merchantUserId}/ratings")
    public Result<PageBean<MerchantRatingVO>> ratings(@PathVariable Integer merchantUserId,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(shopService.getShopRatings(merchantUserId, pageNum, pageSize));
    }
}
