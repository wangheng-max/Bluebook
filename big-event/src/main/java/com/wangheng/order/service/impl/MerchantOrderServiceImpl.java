package com.wangheng.order.service.impl;

import com.wangheng.common.PageBean;
import com.wangheng.order.mapper.OrderItemMapper;
import com.wangheng.order.mapper.OrderMapper;
import com.wangheng.order.mapper.RefundOrderMapper;
import com.wangheng.order.pojo.Order;
import com.wangheng.order.pojo.OrderItem;
import com.wangheng.order.pojo.OrderVO;
import com.wangheng.order.pojo.RefundAuditDTO;
import com.wangheng.order.pojo.RefundOrder;
import com.wangheng.order.pojo.RefundOrderVO;
import com.wangheng.order.service.MerchantOrderService;
import com.wangheng.product.mapper.ProductMapper;
import com.wangheng.product.mapper.ProductSkuMapper;













import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.exception.MerchantAuthException;













import com.wangheng.utils.ThreadLocalUtil;
import com.wangheng.websocket.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 商家订单管理服务实现。
 * 数据隔离：所有操作先校验订单含本店商品（order_item × product.create_user_id）。
 */
@Service
public class MerchantOrderServiceImpl implements MerchantOrderService {

    private static final String ORDER_LIST_CACHE_PREFIX = "order:my-list:";

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private RefundOrderMapper refundOrderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public PageBean<OrderVO> myOrders(Integer pageNum, Integer pageSize, Integer status) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<Order> list = orderMapper.listMerchant(userId, status);
        Page<Order> page = (Page<Order>) list;
        return new PageBean<>(page.getTotal(), toVOList(list));
    }

    @Override
    public OrderVO detail(Integer orderId) {
        Integer userId = currentUserId();
        Order order = requireMerchantOrder(orderId, userId);
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        OrderVO vo = toVO(order, items);
        vo.setRefundStatus(refundOrderMapper.findLatestStatusByOrder(orderId));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ship(Integer orderId) {
        Integer userId = currentUserId();
        Order order = requireMerchantOrder(orderId, userId);
        if (order.getStatus() != 1) {
            throw new RuntimeException("订单当前状态不可发货");
        }
        if (orderMapper.markShipped(orderId) == 0) {
            throw new RuntimeException("订单状态已变化，请刷新");
        }
        evictUserOrderCaches(order.getUserId());
        notifyOrder(order.getUserId(), orderId, "订单已发货，请耐心等待");
    }

    @Override
    public PageBean<RefundOrderVO> refundList(Integer pageNum, Integer pageSize) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<RefundOrderVO> list = refundOrderMapper.listMerchant(userId);
        Page<RefundOrderVO> page = (Page<RefundOrderVO>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void auditRefund(Integer refundId, RefundAuditDTO dto) {
        Integer userId = currentUserId();
        RefundOrder refund = refundOrderMapper.findById(refundId);
        if (refund == null) {
            throw new RuntimeException("退款单不存在");
        }
        Order order = requireMerchantOrder(refund.getOrderId(), userId);
        if (refund.getRefundStatus() != 0) {
            throw new RuntimeException("该退款单已处理");
        }

        boolean approved = Boolean.TRUE.equals(dto.getApproved());
        if (approved) {
            // 通过：退款单 0→3、订单 5→6、回补库存
            refundOrderMapper.audit(refundId, 3, dto.getRemark());
            refundOrderMapper.markRefundedTime(refundId);
            orderMapper.markRefunded(order.getId());
            restoreStocks(order.getId());
            notifyOrder(order.getUserId(), order.getId(), "您的退款申请已通过，款项将原路退回");
        } else {
            // 拒绝：退款单 0→2、订单恢复原状态（已发货→2，未发货→1）
            refundOrderMapper.audit(refundId, 2, dto.getRemark());
            orderMapper.restoreFromRefunding(order.getId(), order.getShipTime() == null ? 1 : 2);
            notifyOrder(order.getUserId(), order.getId(), "您的退款申请被拒绝"
                    + (dto.getRemark() == null ? "" : "：" + dto.getRemark()));
        }
        evictUserOrderCaches(order.getUserId());
    }

    /** 回补库存（退款通过时），回补后失效商品缓存，避免页面显示旧库存 */
    private void restoreStocks(Integer orderId) {
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
            if (item.getSkuId() != null) {
                productSkuMapper.restoreStock(item.getSkuId(), item.getQuantity());
            }
        }
        try {
            var keys = stringRedisTemplate.keys("product:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }

    /** 校验订单含本店商品（数据隔离） */
    private Order requireMerchantOrder(Integer orderId, Integer userId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (orderItemMapper.countByOrderAndMerchant(orderId, userId) == 0) {
            throw new MerchantAuthException("无权操作他人订单");
        }
        return order;
    }

    private OrderVO toVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setOrderType(order.getOrderType());
        vo.setUserId(order.getUserId());
        vo.setGroupBuyId(order.getGroupBuyId());
        vo.setAddressId(order.getAddressId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setPayAmount(order.getPayAmount());
        vo.setUserCouponId(order.getUserCouponId());
        vo.setStatus(order.getStatus());
        vo.setPayTime(order.getPayTime());
        vo.setShipTime(order.getShipTime());
        vo.setFinishTime(order.getFinishTime());
        vo.setCancelTime(order.getCancelTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setRemark(order.getRemark());
        vo.setCreateTime(order.getCreateTime());
        vo.setItems(items);
        return vo;
    }

    private List<OrderVO> toVOList(List<Order> list) {
        List<OrderVO> result = new ArrayList<>();
        for (Order order : list) {
            result.add(toVO(order, orderItemMapper.findByOrderId(order.getId())));
        }
        return result;
    }

    private void notifyOrder(Integer userId, Integer orderId, String text) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("type", "ORDER_STATUS_CHANGE");
            message.put("orderId", orderId);
            message.put("message", text);
            chatWebSocketHandler.sendToUser(userId, objectMapper.writeValueAsString(message));
        } catch (Exception e) {
            // 通知失败不影响主流程
        }
    }

    private void evictUserOrderCaches(Integer userId) {
        try {
            var keys = stringRedisTemplate.keys(ORDER_LIST_CACHE_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程
        }
    }

    private Integer currentUserId() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = claims == null ? null : (Integer) claims.get("id");
        if (userId == null) {
            throw new MerchantAuthException("请先登录");
        }
        return userId;
    }
}
