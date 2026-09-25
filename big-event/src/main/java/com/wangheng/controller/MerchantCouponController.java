package com.wangheng.controller;

import com.wangheng.anno.RequireMerchant;
import com.wangheng.pojo.CouponStatisticsVO;
import com.wangheng.pojo.CouponStock;
import com.wangheng.pojo.CouponStockDTO;
import com.wangheng.pojo.CouponTypeDTO;
import com.wangheng.pojo.GrantDTO;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Result;
import com.wangheng.service.MerchantCouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家优惠券管理接口（仅认证商家，@RequireMerchant + 数据隔离）。
 */
@RestController
@RequestMapping("/merchant/coupon")
public class MerchantCouponController {

    @Autowired
    private MerchantCouponService merchantCouponService;

    /** 创建优惠券类型（满减/折扣） */
    @RequireMerchant
    @PostMapping("/type")
    public Result createType(@RequestBody @Validated CouponTypeDTO dto) {
        merchantCouponService.createType(dto);
        return Result.success();
    }

    /** 修改优惠券类型（未被进行中活动使用时可改） */
    @RequireMerchant
    @PutMapping("/type/{id}")
    public Result updateType(@PathVariable Integer id, @RequestBody @Validated CouponTypeDTO dto) {
        dto.setId(id);
        merchantCouponService.updateType(dto);
        return Result.success();
    }

    /** 创建发放活动（设库存、抢购时间，同步写 Redis 库存） */
    @RequireMerchant
    @PostMapping("/stock")
    public Result createStock(@RequestBody @Validated CouponStockDTO dto) {
        merchantCouponService.createStock(dto);
        return Result.success();
    }

    /** 定向发放优惠券给指定用户 */
    @RequireMerchant
    @PostMapping("/{stockId}/grant")
    public Result grant(@PathVariable Integer stockId, @RequestBody @Validated GrantDTO dto) {
        merchantCouponService.grant(stockId, dto.getUserId());
        return Result.success();
    }

    /** 商家发放活动列表（分页） */
    @RequireMerchant
    @GetMapping("/stock")
    public Result<PageBean<CouponStock>> myStocks(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  @RequestParam(required = false) Integer status) {
        return Result.success(merchantCouponService.myStocks(pageNum, pageSize, status));
    }

    /** 优惠券发放/领取/使用统计 */
    @RequireMerchant
    @GetMapping("/statistics")
    public Result<CouponStatisticsVO> statistics() {
        return Result.success(merchantCouponService.statistics());
    }
}
