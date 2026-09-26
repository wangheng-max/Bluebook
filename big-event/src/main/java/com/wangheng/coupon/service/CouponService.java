package com.wangheng.coupon.service;

import com.wangheng.common.PageBean;
import com.wangheng.coupon.pojo.CouponStockVO;
import com.wangheng.coupon.pojo.CouponType;
import com.wangheng.coupon.pojo.UserCouponVO;












import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券服务（用户视角）：可用券/我的券/抢购活动列表与详情/抢购记录/取消抢购。
 */
public interface CouponService {

    /** 我的可用优惠券（可传最低消费过滤） */
    List<UserCouponVO> available(BigDecimal minSpend);

    /** 我的优惠券（状态筛选 0可用/1已用/2过期） */
    PageBean<UserCouponVO> myList(Integer pageNum, Integer pageSize, Integer status);

    /** 优惠券类型列表（公开） */
    List<CouponType> typeList();

    /** 抢购活动列表（公开分页，status 缺省 1=进行中） */
    PageBean<CouponStockVO> stockPageList(Integer pageNum, Integer pageSize, Integer status);

    /** 抢购活动详情（公开） */
    CouponStockVO stockDetail(Integer id);

    /** 我的抢购记录（分页） */
    PageBean<UserCouponVO> myRecords(Integer pageNum, Integer pageSize);

    /** 取消抢购：券作废（未使用→已过期），库存回补 */
    void cancelGrab(Integer stockId);
}
