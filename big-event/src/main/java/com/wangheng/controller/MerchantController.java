package com.wangheng.controller;

import com.wangheng.anno.RequireMerchant;
import com.wangheng.pojo.MerchantApplyDTO;
import com.wangheng.pojo.MerchantInfo;
import com.wangheng.pojo.MerchantStatusVO;
import com.wangheng.pojo.Result;
import com.wangheng.pojo.ShopInfoDTO;
import com.wangheng.service.MerchantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家认证接口（v1.1 商家认证机制）。
 * apply/status/re-submit 登录即可访问；info 仅认证商家（@RequireMerchant）。
 */
@RestController
@RequestMapping("/merchant")
public class MerchantController {

    @Autowired
    private MerchantService merchantService;

    /** 提交商家认证申请 */
    @PostMapping("/apply")
    public Result apply(@RequestBody @Validated MerchantApplyDTO dto) {
        merchantService.apply(dto);
        return Result.success();
    }

    /** 被拒绝后重新提交申请 */
    @PostMapping("/apply/re-submit")
    public Result reSubmit(@RequestBody @Validated MerchantApplyDTO dto) {
        merchantService.reSubmit(dto);
        return Result.success();
    }

    /** 查询我的商家认证状态（前端每次登录后调用，决定是否渲染"商家中心"） */
    @GetMapping("/status")
    public Result<MerchantStatusVO> status() {
        return Result.success(merchantService.getStatus());
    }

    /** 获取已认证商家的店铺信息 */
    @RequireMerchant
    @GetMapping("/info")
    public Result<MerchantInfo> info() {
        return Result.success(merchantService.getShopInfo());
    }

    /** 修改店铺信息（店名、头像、描述） */
    @RequireMerchant
    @PutMapping("/info")
    public Result updateInfo(@RequestBody @Validated ShopInfoDTO dto) {
        merchantService.updateShopInfo(dto);
        return Result.success();
    }
}
