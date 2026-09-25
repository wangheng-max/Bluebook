package com.wangheng.controller;

import com.wangheng.pojo.CouponType;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Result;
import com.wangheng.pojo.UserCouponVO;
import com.wangheng.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * 优惠券接口（用户视角）。
 */
@RestController
@RequestMapping("/coupon")
public class CouponController {

    @Autowired
    private CouponService couponService;

    /** 我的可用优惠券（可传最低消费过滤） */
    @GetMapping("/available")
    public Result<List<UserCouponVO>> available(@RequestParam(required = false) BigDecimal minSpend) {
        return Result.success(couponService.available(minSpend));
    }

    /** 我的优惠券（状态筛选 0可用/1已用/2过期） */
    @GetMapping("/my-list")
    public Result<PageBean<UserCouponVO>> myList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize,
                                                 @RequestParam(required = false) Integer status) {
        return Result.success(couponService.myList(pageNum, pageSize, status));
    }

    /** 优惠券类型列表（公开） */
    @GetMapping("/type/list")
    public Result<List<CouponType>> typeList() {
        return Result.success(couponService.typeList());
    }
}
