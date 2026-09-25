package com.wangheng.task;

import com.wangheng.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 团购结算定时任务（设计文档《团购业务规则（成团与退款）》）：每分钟检查到期活动。
 * 成团 → 状态 2（商家可发货）；未成团 → 状态 3 + 已支付订单自动退款。
 */
@Slf4j
@Component
public class GroupBuySettleTask {

    @Autowired
    private OrderService orderService;

    @Scheduled(fixedDelay = 60 * 1000)
    public void settleGroupBuys() {
        try {
            orderService.settleGroupBuys();
        } catch (Exception e) {
            log.error("团购结算任务执行失败", e);
        }
    }
}
