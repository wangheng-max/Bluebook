package com.wangheng.task;

import com.wangheng.mapper.OrderMapper;
import com.wangheng.pojo.Order;
import com.wangheng.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单超时自动取消（设计文档《定时任务设计》）：每分钟扫描待支付超 30 分钟的订单。
 * 每单走独立事务（OrderService.cancelTimeoutOrder），失败不互相影响。
 */
@Slf4j
@Component
public class OrderTimeoutTask {

    private static final int TIMEOUT_MINUTES = 30;

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderService orderService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void cancelTimeoutOrders() {
        List<Order> timeoutOrders = orderMapper.findTimeoutOrders(
                LocalDateTime.now().minusMinutes(TIMEOUT_MINUTES));
        for (Order order : timeoutOrders) {
            try {
                orderService.cancelTimeoutOrder(order.getId());
            } catch (Exception e) {
                log.error("超时订单取消失败: orderId={}", order.getId(), e);
            }
        }
    }
}
