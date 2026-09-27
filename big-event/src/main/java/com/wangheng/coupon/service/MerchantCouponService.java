package com.wangheng.coupon.service;

import com.wangheng.common.PageBean;
import com.wangheng.coupon.pojo.CouponStatisticsVO;
import com.wangheng.coupon.pojo.CouponStock;
import com.wangheng.coupon.pojo.CouponStockDTO;
import com.wangheng.coupon.pojo.CouponTypeDTO;














/**
 * 商家优惠券管理服务（仅认证商家，@RequireMerchant + 数据隔离）。
 */
public interface MerchantCouponService {

    /** 创建优惠券类型（满减/折扣） */
    void createType(CouponTypeDTO dto);

    /** 修改优惠券类型（未被进行中活动使用时可改） */
    void updateType(CouponTypeDTO dto);

    /** 创建发放活动（设库存、抢购时间，同步写 Redis 库存） */
    void createStock(CouponStockDTO dto);

    /** 定向发放优惠券给指定用户 */
    void grant(Integer stockId, Integer targetUserId);

    /** 商家发放活动列表（分页） */
    PageBean<CouponStock> myStocks(Integer pageNum, Integer pageSize, Integer status);

    /** 优惠券发放/领取/使用统计 */
    CouponStatisticsVO statistics();
}
