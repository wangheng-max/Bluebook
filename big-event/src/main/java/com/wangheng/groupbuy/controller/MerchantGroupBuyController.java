package com.wangheng.groupbuy.controller;

import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.groupbuy.pojo.GroupBuy;
import com.wangheng.groupbuy.service.GroupBuyService;









import com.wangheng.anno.RequireMerchant;




import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 商家团购管理接口（商城团购功能，仅认证商家）。
 * 供商家中心"团购管理"页使用：查看本店团购活动与参与人数统计。
 */
@RestController
@RequestMapping("/merchant/group-buy")
public class MerchantGroupBuyController {

    @Autowired
    private GroupBuyService groupBuyService;

    /** 本店团购活动列表（分页，status 可选筛选，缺省全部） */
    @RequireMerchant
    @GetMapping
    public Result<PageBean<GroupBuy>> myActivities(@RequestParam(defaultValue = "1") Integer pageNum,
                                                   @RequestParam(defaultValue = "10") Integer pageSize,
                                                   @RequestParam(required = false) Integer status) {
        return Result.success(groupBuyService.myActivities(pageNum, pageSize, status));
    }
}
