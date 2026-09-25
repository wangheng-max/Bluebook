package com.wangheng.service;

import com.wangheng.pojo.OrderVO;
import com.wangheng.pojo.PageBean;
import com.wangheng.pojo.RefundAuditDTO;
import com.wangheng.pojo.RefundOrderVO;

/**
 * 商家订单管理服务（仅认证商家，@RequireMerchant + 数据隔离：只含本店商品）。
 */
public interface MerchantOrderService {

    /** 本店订单列表（分页，状态筛选） */
    PageBean<OrderVO> myOrders(Integer pageNum, Integer pageSize, Integer status);

    /** 本店订单详情 */
    OrderVO detail(Integer orderId);

    /** 发货（1→2，仅本店商品订单） */
    void ship(Integer orderId);

    /** 本店退款申请列表（分页） */
    PageBean<RefundOrderVO> refundList(Integer pageNum, Integer pageSize);

    /** 退款审核：通过（退款单 0→3、订单 5→6）/ 拒绝（退款单 0→2、订单恢复 1/2） */
    void auditRefund(Integer refundId, RefundAuditDTO dto);
}
