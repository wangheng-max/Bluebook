package com.wangheng.controller;

import com.wangheng.anno.RequireAdmin;
import com.wangheng.pojo.AuditRequestDTO;
import com.wangheng.pojo.MerchantInfo;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.Result;
import com.wangheng.service.AdminMerchantService;
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
 * 管理员商家审核接口（v1.1 商家认证机制，仅管理员 @RequireAdmin）。
 * 路径参数统一为 userId（与封禁接口口径一致，见设计文档）。
 */
@RestController
@RequestMapping("/admin/merchant")
public class AdminMerchantController {

    @Autowired
    private AdminMerchantService adminMerchantService;

    /** 审核列表（分页；status 可选筛选） */
    @RequireAdmin
    @GetMapping("/audit-list")
    public Result<PageBean<MerchantInfo>> auditList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) Integer status) {
        return Result.success(adminMerchantService.auditList(pageNum, pageSize, status));
    }

    /** 按状态筛选（0待审核/1已通过/2已拒绝/3已封禁） */
    @RequireAdmin
    @GetMapping("/audit-list/{status}")
    public Result<PageBean<MerchantInfo>> auditListByStatus(@PathVariable Integer status,
                                                            @RequestParam(defaultValue = "1") Integer pageNum,
                                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(adminMerchantService.auditList(pageNum, pageSize, status));
    }

    /** 审核：通过/拒绝（附审核备注） */
    @RequireAdmin
    @PostMapping("/{userId}/audit")
    public Result audit(@PathVariable Integer userId, @RequestBody @Validated AuditRequestDTO dto) {
        adminMerchantService.audit(userId, dto);
        return Result.success();
    }

    /** 封禁商家（违规处理） */
    @RequireAdmin
    @PostMapping("/{userId}/ban")
    public Result ban(@PathVariable Integer userId) {
        adminMerchantService.ban(userId);
        return Result.success();
    }
}
