package com.wangheng.order.service;

import com.wangheng.common.PageBean;
import com.wangheng.order.pojo.OrderCreateDTO;
import com.wangheng.order.pojo.OrderItem;
import com.wangheng.order.pojo.OrderVO;
import com.wangheng.order.pojo.PayDTO;
import com.wangheng.order.pojo.RefundApplyDTO;
import com.wangheng.order.pojo.RefundOrderVO;
import java.util.List;

/**
 * 订单服务。
 * 状态机（条件更新幂等）：0待支付→1待发货→2已发货→3已完成；0→4已取消；1/2→5退款中→6已退款。
 * 幂等：SETNX 占坑（Controller）+ uk_order_no/uk_order_product 唯一索引 + 条件更新状态机。
 */
public interface OrderService {

    /** 创建订单（普通/团购；扣库存、核销优惠券、写支付流水，同一事务） */
    OrderVO create(OrderCreateDTO dto);

    /** 支付订单（本期模拟支付：校验金额后直接标记支付成功） */
    void pay(Integer orderId, PayDTO dto);

    /** 取消订单（0→4，回补库存、释放优惠券） */
    void cancel(Integer orderId);

    /** 申请退款（1/2→5，创建退款单） */
    void applyRefund(Integer orderId, RefundApplyDTO dto);

    /** 我的订单列表（分页，第 1 页缓存 5 分钟） */
    PageBean<OrderVO> myList(Integer pageNum, Integer pageSize, Integer status);

    /** 订单详情（仅订单所属用户） */
    OrderVO detail(Integer orderId);

    /** 我的退款单列表（分页） */
    PageBean<RefundOrderVO> myRefundList(Integer pageNum, Integer pageSize);

    /** 超时未支付订单自动取消（定时任务，每单独立事务） */
    void cancelTimeoutOrder(Integer orderId);

    /** 团购结算（定时任务）：成团→发货等待；未成团→自动退款 */
    void settleGroupBuys();

    /**
     * 我购买某商品的有效订单明细（状态 1/2/3，商品评论晒单选择用，新的在前）
     */
    List<OrderItem> myPurchasedItems(Integer productId);
}
