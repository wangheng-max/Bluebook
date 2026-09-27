package com.wangheng.groupbuy.controller;

import com.wangheng.anno.RequireMerchant;
import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.groupbuy.pojo.GroupBuy;
import com.wangheng.groupbuy.pojo.GroupBuyDetailVO;
import com.wangheng.groupbuy.pojo.GroupBuyProgressVO;
import com.wangheng.groupbuy.pojo.GroupBuySaveDTO;
import com.wangheng.groupbuy.service.GroupBuyService;
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
 * 团购活动接口（商城团购功能）。
 * 列表/详情公开；创建/修改/关闭仅认证商家；我的团购/进度需登录。
 */
@RestController
@RequestMapping("/group-buy")
public class GroupBuyController {

    @Autowired
    private GroupBuyService groupBuyService;

    /** 团购活动列表（公开分页，status 缺省 1=进行中） */
    @GetMapping
    public Result<PageBean<GroupBuy>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) Integer status) {
        return Result.success(groupBuyService.pageList(pageNum, pageSize, status));
    }

    /** 团购详情（公开） */
    @GetMapping("/{id}")
    public Result<GroupBuyDetailVO> detail(@PathVariable Integer id) {
        return Result.success(groupBuyService.detail(id));
    }

    /** 我的团购（已参与，分页） */
    @GetMapping("/my-list")
    public Result<PageBean<GroupBuy>> myList(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(groupBuyService.myList(pageNum, pageSize));
    }

    /** 我的团购进度 */
    @GetMapping("/my-progress")
    public Result<List<GroupBuyProgressVO>> myProgress() {
        return Result.success(groupBuyService.myProgress());
    }

    /** 创建团购活动（仅认证商家，商品须为本店商品） */
    @RequireMerchant
    @PostMapping
    public Result create(@RequestBody @Validated GroupBuySaveDTO dto) {
        groupBuyService.create(dto);
        return Result.success();
    }

    /** 修改团购活动（仅认证商家，仅未开始可改） */
    @RequireMerchant
    @PutMapping("/{id}")
    public Result update(@PathVariable Integer id, @RequestBody @Validated GroupBuySaveDTO dto) {
        dto.setId(id);
        groupBuyService.update(dto);
        return Result.success();
    }

    /** 关闭团购活动（仅认证商家，已成团不可关） */
    @RequireMerchant
    @DeleteMapping("/{id}")
    public Result close(@PathVariable Integer id) {
        groupBuyService.close(id);
        return Result.success();
    }
}
