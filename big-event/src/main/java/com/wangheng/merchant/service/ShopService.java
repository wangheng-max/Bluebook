package com.wangheng.merchant.service;

import com.wangheng.common.PageBean;
import com.wangheng.merchant.pojo.MerchantRatingVO;
import com.wangheng.merchant.pojo.ShopVO;
import com.wangheng.product.pojo.Product;


/**
 * 店铺主页服务（公开：店铺信息 / 在架商品 / 评价列表）。
 */
public interface ShopService {

    /** 店铺信息 + 评分汇总 + 商品数 */
    ShopVO getShop(Integer merchantUserId);

    /** 店铺在架商品（分页） */
    PageBean<Product> getShopProducts(Integer merchantUserId, Integer pageNum, Integer pageSize);

    /** 店铺评价（分页） */
    PageBean<MerchantRatingVO> getShopRatings(Integer merchantUserId, Integer pageNum, Integer pageSize);
}
