package com.wangheng.order.service.impl;

import com.wangheng.address.mapper.UserAddressMapper;
import com.wangheng.common.PageBean;
import com.wangheng.coupon.mapper.CouponTypeMapper;
import com.wangheng.coupon.mapper.UserCouponMapper;
import com.wangheng.coupon.pojo.CouponType;
import com.wangheng.coupon.pojo.UserCoupon;
import com.wangheng.groupbuy.mapper.GroupBuyMapper;
import com.wangheng.groupbuy.mapper.GroupBuyRecordMapper;
import com.wangheng.groupbuy.pojo.GroupBuy;
import com.wangheng.groupbuy.pojo.GroupBuyRecord;
import com.wangheng.order.mapper.OrderItemMapper;
import com.wangheng.order.mapper.OrderMapper;
import com.wangheng.order.mapper.PaymentLogMapper;
import com.wangheng.order.mapper.RefundOrderMapper;
import com.wangheng.order.pojo.Order;
import com.wangheng.order.pojo.OrderCreateDTO;
import com.wangheng.order.pojo.OrderItem;
import com.wangheng.order.pojo.OrderItemDTO;
import com.wangheng.order.pojo.OrderVO;
import com.wangheng.order.pojo.PayDTO;
import com.wangheng.order.pojo.PaymentLog;
import com.wangheng.order.pojo.RefundApplyDTO;
import com.wangheng.order.pojo.RefundOrder;
import com.wangheng.order.pojo.RefundOrderVO;
import com.wangheng.order.service.OrderService;
import com.wangheng.product.mapper.ProductMapper;
import com.wangheng.product.mapper.ProductSkuMapper;
import com.wangheng.product.pojo.Product;
import com.wangheng.product.pojo.ProductSku;

























import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.wangheng.exception.MerchantAuthException;





























import com.wangheng.utils.ThreadLocalUtil;
import com.wangheng.websocket.ChatWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 订单服务实现。
 * 库存扣减用条件更新（WHERE stock>=qty）防超卖；状态流转用条件更新保证幂等；
 * 团购规则：支付成功写入参团记录；活动到期未成团由结算任务自动退款（设计文档《团购业务规则》）。
 */
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {

    private static final String ORDER_LIST_CACHE_PREFIX = "order:my-list:";
    private static final String USER_COUPON_CACHE_PREFIX = "coupon:user:available:";
    private static final String GROUP_BUY_KEY_PREFIX = "group-buy:";
    private static final long ORDER_LIST_TTL_MINUTES = 5;
    private static final long ORDER_TIMEOUT_MINUTES = 30;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private PaymentLogMapper paymentLogMapper;

    @Autowired
    private RefundOrderMapper refundOrderMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductSkuMapper productSkuMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private CouponTypeMapper couponTypeMapper;

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private GroupBuyMapper groupBuyMapper;

    @Autowired
    private GroupBuyRecordMapper groupBuyRecordMapper;

    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO create(OrderCreateDTO dto) {
        Integer userId = currentUserId();
        if (dto.getOrderType() == null || (dto.getOrderType() != 1 && dto.getOrderType() != 2)) {
            throw new RuntimeException("订单类型不正确");
        }
        if (userAddressMapper.countOwned(dto.getAddressId(), userId) == 0) {
            throw new RuntimeException("收货地址不存在");
        }

        // 团购订单：校验活动 + 防重复参团（参团记录支付后才写入，需另查订单表防重复下单锁库存）
        GroupBuy groupBuy = null;
        if (dto.getOrderType() == 2) {
            if (dto.getGroupBuyId() == null) {
                throw new RuntimeException("团购订单必须指定团购活动");
            }
            groupBuy = groupBuyMapper.findById(dto.getGroupBuyId());
            if (groupBuy == null) {
                throw new RuntimeException("团购活动不存在");
            }
            refreshGroupBuyStatus(groupBuy);
            if (groupBuy.getStatus() != 1) {
                throw new RuntimeException("团购活动未开始或已结束");
            }
            if (groupBuyRecordMapper.countByGroupBuyId(groupBuy.getId()) >= groupBuy.getGroupSize()) {
                throw new RuntimeException("该团购人数已满，无法再加入");
            }
            if (orderMapper.countUserGroupOrders(userId, groupBuy.getId()) > 0) {
                throw new RuntimeException("您已参与过该团购，请先完成支付或取消已有订单");
            }
            // 单笔限购：参团数量不超过成团门槛，防止单人锁死全部库存
            for (OrderItemDTO itemDto : dto.getProductItems()) {
                if (itemDto.getQuantity() != null && itemDto.getQuantity() > groupBuy.getGroupSize()) {
                    throw new RuntimeException("团购单笔限购 " + groupBuy.getGroupSize() + " 件");
                }
            }
        }

        // 逐项校验 + 条件扣库存（防超卖）+ 计算金额
        List<OrderItem> items = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItemDTO itemDto : dto.getProductItems()) {
            Product product = productMapper.findById(itemDto.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new RuntimeException("商品不存在或已下架");
            }
            ProductSku sku = null;
            if (itemDto.getSkuId() != null) {
                sku = productSkuMapper.findById(itemDto.getSkuId());
                if (sku == null || !sku.getProductId().equals(product.getId())) {
                    throw new RuntimeException("SKU 不存在或不属于该商品");
                }
            }
            BigDecimal price = groupBuy != null ? groupBuy.getGroupPrice()
                    : (sku != null ? sku.getPrice() : product.getPrice());
            int qty = itemDto.getQuantity();
            if (productMapper.deductStock(product.getId(), qty) == 0) {
                throw new RuntimeException("商品库存不足：" + product.getName());
            }
            if (sku != null && productSkuMapper.deductStock(sku.getId(), qty) == 0) {
                throw new RuntimeException("SKU 库存不足：" + sku.getSkuName());
            }

            OrderItem item = new OrderItem();
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setSkuId(sku == null ? null : sku.getId());
            item.setSkuName(sku == null ? null : sku.getSkuName());
            item.setProductImg(product.getCoverImg());
            item.setPrice(price);
            item.setQuantity(qty);
            item.setTotalPrice(price.multiply(BigDecimal.valueOf(qty)));
            item.setCouponDiscount(BigDecimal.ZERO);
            items.add(item);
            totalAmount = totalAmount.add(item.getTotalPrice());
        }

        // 优惠券校验与抵扣（核销在插入订单后，条件更新防并发重复使用）
        BigDecimal couponDiscount = BigDecimal.ZERO;
        if (dto.getUserCouponId() != null) {
            UserCoupon uc = userCouponMapper.findById(dto.getUserCouponId());
            if (uc == null || !uc.getUserId().equals(userId) || uc.getStatus() == null || uc.getStatus() != 0) {
                throw new RuntimeException("优惠券不可用");
            }
            if (uc.getEndTime() == null || uc.getEndTime().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("优惠券已过期");
            }
            CouponType type = couponTypeMapper.findById(uc.getCouponTypeId());
            if (type == null) {
                throw new RuntimeException("优惠券类型不存在");
            }
            if (type.getMinSpend() != null && totalAmount.compareTo(type.getMinSpend()) < 0) {
                throw new RuntimeException("未达到优惠券使用门槛");
            }
            if (type.getDiscountAmount() != null && type.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                couponDiscount = type.getDiscountAmount().min(totalAmount);
            } else if (type.getDiscountRate() != null && type.getDiscountRate().compareTo(BigDecimal.ZERO) > 0
                    && type.getDiscountRate().compareTo(BigDecimal.ONE) < 0) {
                couponDiscount = totalAmount.multiply(BigDecimal.ONE.subtract(type.getDiscountRate()))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            if (!items.isEmpty()) {
                items.get(0).setCouponDiscount(couponDiscount);
            }
        }

        // 订单 + 明细 + 支付流水（待支付）
        Order order = new Order();
        order.setOrderNo(generateOrderNo());
        order.setOrderType(dto.getOrderType());
        order.setUserId(userId);
        order.setGroupBuyId(groupBuy == null ? null : groupBuy.getId());
        order.setAddressId(dto.getAddressId());
        order.setTotalAmount(totalAmount);
        order.setPayAmount(totalAmount.subtract(couponDiscount));
        order.setUserCouponId(dto.getUserCouponId());
        order.setRemark(dto.getRemark());
        orderMapper.insert(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemMapper.insert(item);
        }
        PaymentLog log = new PaymentLog();
        log.setOrderId(order.getId());
        log.setPayAmount(order.getPayAmount());
        paymentLogMapper.insert(log);

        // 核销优惠券（条件更新；失败说明并发下已被他单使用，整体回滚）
        if (dto.getUserCouponId() != null) {
            if (userCouponMapper.markUsed(dto.getUserCouponId(), order.getId(), userId) == 0) {
                throw new RuntimeException("优惠券已被使用");
            }
        }
        evictCaches(userId);
        evictProductCaches();
        return toVO(order, items, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void pay(Integer orderId, PayDTO dto) {
        Integer userId = currentUserId();
        Order order = requireOwnOrder(orderId, userId);
        if (order.getStatus() != 0) {
            throw new RuntimeException("订单状态不可支付");
        }
        if (orderMapper.markPaid(orderId) == 0) {
            throw new RuntimeException("订单状态已变化，请刷新");
        }
        paymentLogMapper.markPaid(orderId, dto.getPayType(), "MOCK" + orderId + System.currentTimeMillis());

        // 团购订单：支付前复检活动状态（下单后活动可能已结算），满员即时成团
        if (order.getGroupBuyId() != null) {
            GroupBuy groupBuy = groupBuyMapper.findById(order.getGroupBuyId());
            if (groupBuy != null) {
                refreshGroupBuyStatus(groupBuy);
                if (groupBuy.getStatus() != 1) {
                    throw new RuntimeException("团购活动已结束，订单无法支付，请取消订单");
                }
            }
            List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
            int qty = 0;
            for (OrderItem item : items) {
                qty += item.getQuantity() == null ? 1 : item.getQuantity();
            }
            GroupBuyRecord record = new GroupBuyRecord();
            record.setGroupBuyId(order.getGroupBuyId());
            record.setOrderId(orderId);
            record.setUserId(userId);
            record.setGroupNum(qty);
            try {
                groupBuyRecordMapper.insert(record);
            } catch (DuplicateKeyException e) {
                throw new RuntimeException("您已参与过该团购");
            }
            int joined = groupBuyRecordMapper.countByGroupBuyId(order.getGroupBuyId());
            groupBuyMapper.updateProgress(order.getGroupBuyId(), joined);
            if (groupBuy != null && joined >= groupBuy.getGroupSize()) {
                groupBuyMapper.updateStatus(order.getGroupBuyId(), 2);
                notifyParticipants(groupBuy, "您参与的团购已成团，商家将尽快发货");
            }
            evictGroupBuyCaches(order.getGroupBuyId());
        }
        evictCaches(userId);
        notifyOrder(userId, orderId, "订单已支付，请等待发货");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Integer orderId) {
        Integer userId = currentUserId();
        Order order = requireOwnOrder(orderId, userId);
        if (!cancelInternal(order, "用户取消")) {
            throw new RuntimeException("订单状态不可取消");
        }
        evictCaches(userId);
        notifyOrder(userId, orderId, "订单已取消");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyRefund(Integer orderId, RefundApplyDTO dto) {
        Integer userId = currentUserId();
        Order order = requireOwnOrder(orderId, userId);
        if (order.getStatus() != 1 && order.getStatus() != 2) {
            throw new RuntimeException("当前状态不可申请退款");
        }
        if (refundOrderMapper.countPendingByOrder(orderId) > 0) {
            throw new RuntimeException("已有退款申请处理中");
        }
        RefundOrder refund = new RefundOrder();
        refund.setOrderId(orderId);
        refund.setRefundAmount(order.getPayAmount());
        refund.setRefundReason(dto.getRefundReason());
        refundOrderMapper.insert(refund);
        orderMapper.markRefunding(orderId);
        evictCaches(userId);
        notifyOrder(userId, orderId, "退款申请已提交，等待商家处理");
    }

    @Override
    public PageBean<OrderVO> myList(Integer pageNum, Integer pageSize, Integer status) {
        Integer userId = currentUserId();
        String cacheKey = ORDER_LIST_CACHE_PREFIX + userId + ":" + (status == null ? "all" : status);

        if (pageNum == 1) {
            PageBean<OrderVO> cached = readCache(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        PageHelper.startPage(pageNum, pageSize);
        List<Order> list = orderMapper.myList(userId, status);
        Page<Order> page = (Page<Order>) list;
        PageBean<OrderVO> bean = new PageBean<>(page.getTotal(), toVOList(list));

        if (pageNum == 1) {
            writeCache(cacheKey, bean, ORDER_LIST_TTL_MINUTES);
        }
        return bean;
    }

    @Override
    public OrderVO detail(Integer orderId) {
        Integer userId = currentUserId();
        Order order = requireOwnOrder(orderId, userId);
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        return toVO(order, items, refundOrderMapper.findLatestStatusByOrder(orderId));
    }

    @Override
    public PageBean<RefundOrderVO> myRefundList(Integer pageNum, Integer pageSize) {
        Integer userId = currentUserId();
        PageHelper.startPage(pageNum, pageSize);
        List<RefundOrderVO> list = refundOrderMapper.myList(userId);
        Page<RefundOrderVO> page = (Page<RefundOrderVO>) list;
        return new PageBean<>(page.getTotal(), page.getResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTimeoutOrder(Integer orderId) {
        Order order = orderMapper.findById(orderId);
        if (order == null || order.getStatus() != 0) {
            return;
        }
        if (cancelInternal(order, "超时未支付")) {
            evictCaches(order.getUserId());
            notifyOrder(order.getUserId(), orderId, "订单已超时自动取消");
        }
    }

    @Override
    public void settleGroupBuys() {
        // 到期未结算的进行中活动
        List<GroupBuy> active = groupBuyMapper.list(1);
        LocalDateTime now = LocalDateTime.now();
        for (GroupBuy g : active) {
            if (g.getEndTime() == null || g.getEndTime().isAfter(now)) {
                continue;
            }
            try {
                int joined = groupBuyRecordMapper.countByGroupBuyId(g.getId());
                if (joined >= g.getGroupSize()) {
                    // 成团：商家可发货
                    groupBuyMapper.updateStatus(g.getId(), 2);
                    notifyParticipants(g, "您参与的团购已成团，商家将尽快发货");
                } else {
                    // 未成团：自动退款
                    groupBuyMapper.updateStatus(g.getId(), 3);
                    autoRefundGroupBuy(g);
                    notifyParticipants(g, "团购未成团，已为您自动退款");
                }
                evictGroupBuyCaches(g.getId());
            } catch (Exception e) {
                log.error("团购结算失败: groupBuyId={}", g.getId(), e);
            }
        }
    }

    /** 未成团自动退款：订单 1→6 + 退款单置 3 + 回补库存 */
    private void autoRefundGroupBuy(GroupBuy g) {
        List<GroupBuyRecord> records = groupBuyRecordMapper.findByGroupBuyId(g.getId());
        for (GroupBuyRecord record : records) {
            Order order = orderMapper.findById(record.getOrderId());
            if (order == null || order.getStatus() != 1) {
                continue;
            }
            orderMapper.markGroupRefunded(order.getId());
            RefundOrder refund = new RefundOrder();
            refund.setOrderId(order.getId());
            refund.setRefundAmount(order.getPayAmount());
            refund.setRefundReason("团购未成团自动退款");
            refundOrderMapper.insert(refund);
            refundOrderMapper.markAutoRefunded(refund.getId(), "团购未成团自动退款");
            restoreStocks(order.getId());
            evictCaches(order.getUserId());
            notifyOrder(order.getUserId(), order.getId(), "团购未成团，订单已自动退款");
        }
    }

    /** 取消核心：0→4、回补库存、释放优惠券；返回是否成功 */
    private boolean cancelInternal(Order order, String reason) {
        if (orderMapper.markCancelled(order.getId(), reason) == 0) {
            return false;
        }
        restoreStocks(order.getId());
        if (order.getUserCouponId() != null) {
            userCouponMapper.restore(order.getUserCouponId());
        }
        return true;
    }

    /** 按明细回补商品/SKU 库存（取消/超时/团购未成团退款共用），回补后失效商品缓存 */
    private void restoreStocks(Integer orderId) {
        List<OrderItem> items = orderItemMapper.findByOrderId(orderId);
        for (OrderItem item : items) {
            productMapper.restoreStock(item.getProductId(), item.getQuantity());
            if (item.getSkuId() != null) {
                productSkuMapper.restoreStock(item.getSkuId(), item.getQuantity());
            }
        }
        evictProductCaches();
    }

    private Order requireOwnOrder(Integer orderId, Integer userId) {
        Order order = orderMapper.findById(orderId);
        if (order == null) {
            throw new RuntimeException("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new MerchantAuthException("无权操作他人订单");
        }
        return order;
    }

    /** 团购状态懒刷新（下单校验用） */
    private void refreshGroupBuyStatus(GroupBuy g) {
        LocalDateTime now = LocalDateTime.now();
        if (g.getStatus() != null && g.getStatus() == 0 && !now.isBefore(g.getStartTime())) {
            g.setStatus(1);
            groupBuyMapper.updateStatus(g.getId(), 1);
        }
        if (g.getStatus() != null && g.getStatus() == 1 && now.isAfter(g.getEndTime())) {
            int joined = groupBuyRecordMapper.countByGroupBuyId(g.getId());
            int newStatus = joined >= g.getGroupSize() ? 2 : 3;
            g.setStatus(newStatus);
            groupBuyMapper.updateStatus(g.getId(), newStatus);
        }
    }

    private OrderVO toVO(Order order, List<OrderItem> items, Integer refundStatus) {
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
        vo.setRefundStatus(refundStatus);
        vo.setItems(items);
        return vo;
    }

    private List<OrderVO> toVOList(List<Order> list) {
        List<OrderVO> result = new ArrayList<>();
        for (Order order : list) {
            List<OrderItem> items = orderItemMapper.findByOrderId(order.getId());
            result.add(toVO(order, items, refundOrderMapper.findLatestStatusByOrder(order.getId())));
        }
        return result;
    }

    private String generateOrderNo() {
        return "O" + System.currentTimeMillis()
                + String.format("%04d", (int) (Math.random() * 10000));
    }

    /** 团购结束通知参与用户 */
    private void notifyParticipants(GroupBuy g, String text) {
        List<GroupBuyRecord> records = groupBuyRecordMapper.findByGroupBuyId(g.getId());
        for (GroupBuyRecord record : records) {
            try {
                Map<String, Object> message = new HashMap<>();
                message.put("type", "GROUP_BUY_PROGRESS");
                message.put("groupBuyId", g.getId());
                message.put("message", text);
                chatWebSocketHandler.sendToUser(record.getUserId(), objectMapper.writeValueAsString(message));
            } catch (Exception e) {
                // 单个用户通知失败不影响结算
            }
        }
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

    private void evictCaches(Integer userId) {
        try {
            stringRedisTemplate.delete(USER_COUPON_CACHE_PREFIX + userId);
            var keys = stringRedisTemplate.keys(ORDER_LIST_CACHE_PREFIX + userId + ":*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }

    private void evictGroupBuyCaches(Integer groupBuyId) {
        try {
            stringRedisTemplate.delete(GROUP_BUY_KEY_PREFIX + groupBuyId);
            var keys = stringRedisTemplate.keys(GROUP_BUY_KEY_PREFIX + "list:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 忽略
        }
    }

    /**
     * 库存变动后失效商品缓存（详情 product:{id}、列表 product:list:*、搜索 product:search:*）。
     * 不失效的话页面最长 5 分钟显示旧库存，看起来像"下单没扣库存/退款没回补"。
     */
    private void evictProductCaches() {
        try {
            var keys = stringRedisTemplate.keys("product:*");
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        } catch (Exception e) {
            // 缓存失效失败不影响主流程，TTL 到期自然过期
        }
    }

    private PageBean<OrderVO> readCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<PageBean<OrderVO>>() {});
            }
        } catch (Exception e) {
            // 数据损坏按未命中处理
        }
        return null;
    }

    private void writeCache(String key, PageBean<OrderVO> bean, long ttlMinutes) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(bean),
                    ttlMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            // 缓存写入失败不影响业务
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
