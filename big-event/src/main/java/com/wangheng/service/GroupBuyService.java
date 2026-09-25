package com.wangheng.service;

import com.wangheng.pojo.GroupBuy;
import com.wangheng.pojo.GroupBuyDetailVO;
import com.wangheng.pojo.GroupBuyProgressVO;
import com.wangheng.pojo.GroupBuySaveDTO;
import com.wangheng.pojo.PageBean;

import java.util.List;

/**
 * 团购活动服务。
 * 列表/详情公开；创建/修改/关闭仅认证商家（@RequireMerchant + 归属校验）；
 * 业务规则见设计文档《团购业务规则（成团与退款）》——成团发货、未成团自动退款（订单模块配合）。
 */
public interface GroupBuyService {

    /** 团购活动列表（公开分页；status 缺省 1=进行中） */
    PageBean<GroupBuy> pageList(Integer pageNum, Integer pageSize, Integer status);

    /** 团购详情（公开；含商品摘要、我的参团情况） */
    GroupBuyDetailVO detail(Integer id);

    /** 我的团购（已参与的活动，分页） */
    PageBean<GroupBuy> myList(Integer pageNum, Integer pageSize);

    /** 我的团购进度（已参与活动 + 当前进度） */
    List<GroupBuyProgressVO> myProgress();

    /** 创建团购活动（仅认证商家，商品须为本店商品） */
    void create(GroupBuySaveDTO dto);

    /** 修改团购活动（仅认证商家，仅未开始可改） */
    void update(GroupBuySaveDTO dto);

    /** 关闭团购活动（仅认证商家；已成团不可关） */
    void close(Integer id);

    /** 商家自己的团购活动列表（分页） */
    PageBean<GroupBuy> myActivities(Integer pageNum, Integer pageSize, Integer status);
}
