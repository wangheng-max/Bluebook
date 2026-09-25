package com.wangheng.service;

import com.wangheng.pojo.MerchantApplyDTO;
import com.wangheng.pojo.MerchantInfo;
import com.wangheng.pojo.MerchantStatusVO;
import com.wangheng.pojo.ShopInfoDTO;

/**
 * 商家认证服务（v1.1 商家认证机制）
 */
public interface MerchantService {

    /** 提交商家认证申请（待审核/已通过/已封禁状态下拒绝重复提交；已拒绝时允许覆盖重提） */
    void apply(MerchantApplyDTO dto);

    /** 被拒绝后重新提交认证申请 */
    void reSubmit(MerchantApplyDTO dto);

    /** 查询我的商家认证状态（前端据此显示/隐藏商家管理入口） */
    MerchantStatusVO getStatus();

    /** 获取已认证商家的店铺信息（仅认证商家，@RequireMerchant 保护） */
    MerchantInfo getShopInfo();

    /** 修改店铺信息（店名、头像、描述） */
    void updateShopInfo(ShopInfoDTO dto);
}
