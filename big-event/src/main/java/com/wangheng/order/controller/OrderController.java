package com.wangheng.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wangheng.common.PageBean;
import com.wangheng.common.Result;
import com.wangheng.order.pojo.OrderCreateDTO;
import com.wangheng.order.pojo.OrderItem;
import com.wangheng.order.pojo.OrderVO;
import com.wangheng.order.pojo.PayDTO;
import com.wangheng.order.pojo.RefundApplyDTO;
import com.wangheng.order.pojo.RefundOrderVO;
import com.wangheng.order.service.OrderService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 订单接口（商城团购功能）。
 * 创建订单幂等：SETNX 占坑 + 业务失败释放占坑（设计文档《幂等性设计》）。
 */
@RestController
@RequestMapping("/order")
public class OrderController {

    private static final String IDEMPOTENT_KEY_PREFIX = "idempotent:order:";

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    /** 我的订单列表（分页，status 可选筛选） */
    @GetMapping
    public Result<PageBean<OrderVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize,
                                          @RequestParam(required = false) Integer status) {
        return Result.success(orderService.myList(pageNum, pageSize, status));
    }

    /** 我的订单列表（别名，与 GET /order 等价） */
    @GetMapping("/my-list")
    public Result<PageBean<OrderVO>> myList(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) Integer status) {
        return Result.success(orderService.myList(pageNum, pageSize, status));
    }

    /** 订单详情 */
    @GetMapping("/{id}")
    public Result<OrderVO> detail(@PathVariable Integer id) {
        return Result.success(orderService.detail(id));
    }

    /** 我的退款单列表 */
    @GetMapping("/refund-list")
    public Result<PageBean<RefundOrderVO>> refundList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(orderService.myRefundList(pageNum, pageSize));
    }

    /** 创建订单（幂等：Idempotency-Key 头，缺省服务端兜底生成） */
    @PostMapping
    public Result<OrderVO> create(@RequestBody @Validated OrderCreateDTO dto,
                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            idempotencyKey = UUID.randomUUID().toString();
        }
        String idemKey = IDEMPOTENT_KEY_PREFIX + idempotencyKey;
        try {
            Boolean grabbed = stringRedisTemplate.opsForValue().setIfAbsent(idemKey, "processing", 1, TimeUnit.HOURS);
            if (!Boolean.TRUE.equals(grabbed)) {
                String cached = stringRedisTemplate.opsForValue().get(idemKey);
                if (cached != null && !"processing".equals(cached)) {
                    // 重复请求：返回首次处理结果
                    return Result.success(objectMapper.readValue(cached, OrderVO.class));
                }
                return Result.error("请勿重复提交订单");
            }
        } catch (Exception e) {
            // Redis 不可用时跳过幂等占坑（uk_order_no 唯一索引仍兜底）
        }

        OrderVO vo;
        try {
            vo = orderService.create(dto);
        } catch (Exception e) {
            // 业务失败释放占坑，允许客户端换新幂等键重试
            try {
                stringRedisTemplate.delete(idemKey);
            } catch (Exception ignored) {
            }
            throw e;
        }
        try {
            stringRedisTemplate.opsForValue().set(idemKey, objectMapper.writeValueAsString(vo), 1, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
        return Result.success(vo);
    }

    /** 支付订单（模拟支付） */
    @PostMapping("/{id}/pay")
    public Result pay(@PathVariable Integer id, @RequestBody @Validated PayDTO dto) {
        orderService.pay(id, dto);
        return Result.success();
    }

    /** 取消订单 */
    @PutMapping("/{id}/cancel")
    public Result cancel(@PathVariable Integer id) {
        orderService.cancel(id);
        return Result.success();
    }

    /** 申请退款 */
    @PostMapping("/{id}/refund")
    public Result refund(@PathVariable Integer id, @RequestBody RefundApplyDTO dto) {
        orderService.applyRefund(id, dto);
        return Result.success();
    }

    /** 我购买某商品的订单明细（商品评论晒单：自动带上买了什么） */
    @GetMapping("/purchased")
    public Result<List<OrderItem>> purchased(@RequestParam Integer productId) {
        return Result.success(orderService.myPurchasedItems(productId));
    }
}
