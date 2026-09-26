package com.wangheng.admin.service;

import com.wangheng.admin.pojo.AuditRequestDTO;
import com.wangheng.common.PageBean;
import com.wangheng.merchant.pojo.MerchantInfo;













/**
 * 管理员商家审核服务（v1.1 商家认证机制）
 */
public interface AdminMerchantService {

    /** 商家认证审核列表（分页；status 为空查全部） */
    PageBean<MerchantInfo> auditList(Integer pageNum, Integer pageSize, Integer status);

    /** 审核：通过/拒绝（同一事务：认证状态 + 商家角色生效标记 + 缓存失效 + WebSocket 通知） */
    void audit(Integer userId, AuditRequestDTO dto);

    /** 封禁商家（角色立即失效） */
    void ban(Integer userId);
}
