package com.wangheng.controller;

import com.wangheng.anno.RequireMerchant;
import com.wangheng.pojo.OrderVO;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.RefundAuditDTO;
import com.wangheng.pojo.RefundOrderVO;
import com.wangheng.pojo.Result;
import com.wangheng.service.MerchantOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家订单管理接口（仅认证商家，数据隔离：只含本店商品）。
 */
@RestController
@RequestMapping("/merchant")
public class MerchantOrderController {

    @Autowired
    private MerchantOrderService merchantOrderService;

    /** 本店订单列表（分页，状态筛选） */
    @RequireMerchant
    @GetMapping("/order")
    public Result<PageBean<OrderVO>> myOrders(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) Integer status) {
        return Result.success(merchantOrderService.myOrders(pageNum, pageSize, status));
    }

    /** 本店订单详情 */
    @RequireMerchant
    @GetMapping("/order/{id}")
    public Result<OrderVO> detail(@PathVariable Integer id) {
        return Result.success(merchantOrderService.detail(id));
    }

    /** 发货 */
    @RequireMerchant
    @PostMapping("/order/{id}/ship")
    public Result ship(@PathVariable Integer id) {
        merchantOrderService.ship(id);
        return Result.success();
    }

    /** 本店退款申请列表 */
    @RequireMerchant
    @GetMapping("/refund/list")
    public Result<PageBean<RefundOrderVO>> refundList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(merchantOrderService.refundList(pageNum, pageSize));
    }

    /** 退款审核：通过/拒绝 */
    @RequireMerchant
    @PostMapping("/refund/{id}/audit")
    public Result auditRefund(@PathVariable Integer id, @RequestBody @Validated RefundAuditDTO dto) {
        merchantOrderService.auditRefund(id, dto);
        return Result.success();
    }
}
