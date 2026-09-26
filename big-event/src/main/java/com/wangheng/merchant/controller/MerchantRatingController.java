package com.wangheng.merchant.controller;

import com.wangheng.common.Result;
import com.wangheng.merchant.mapper.MerchantInfoMapper;
import com.wangheng.merchant.mapper.MerchantRatingMapper;
import com.wangheng.merchant.pojo.MerchantInfo;
import com.wangheng.merchant.pojo.RatingCreateDTO;
import com.wangheng.order.mapper.OrderMapper;
import com.wangheng.order.pojo.Order;




















import com.wangheng.utils.ThreadLocalUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 商家评分接口（登录用户，订单维度一单一评）。
 * 买家在订单已发货(2)/已完成(3)后可评价；评价对象由订单明细商品的创建者解析。
 */
@RestController
@RequestMapping("/merchant/rating")
@Validated
public class MerchantRatingController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private MerchantInfoMapper merchantInfoMapper;

    @Autowired
    private MerchantRatingMapper merchantRatingMapper;

    /** 提交评价 */
    @PostMapping
    public Result rate(@RequestBody @Valid RatingCreateDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");

        Order order = orderMapper.findById(dto.getOrderId());
        if (order == null) {
            return Result.error("订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            return Result.error("只能评价自己的订单");
        }
        if (order.getStatus() == null || (order.getStatus() != 2 && order.getStatus() != 3)) {
            return Result.error("订单已发货后才能评价");
        }
        Integer merchantUserId = orderMapper.findMerchantUserIdByOrderId(dto.getOrderId());
        if (merchantUserId == null) {
            return Result.error("订单未关联店铺，无法评价");
        }
        MerchantInfo shop = merchantInfoMapper.findByUserId(merchantUserId);
        if (shop == null || !Integer.valueOf(1).equals(shop.getMerchantStatus())) {
            return Result.error("该店铺不可评价");
        }
        try {
            merchantRatingMapper.insert(merchantUserId, dto.getOrderId(), userId,
                    dto.getScore(), dto.getContent());
        } catch (DuplicateKeyException e) {
            return Result.error("该订单已评价过");
        }
        return Result.success();
    }

    /** 订单是否已评价（我的订单页显示"评价/已评价"按钮用） */
    @GetMapping("/exists")
    public Result<Boolean> exists(@RequestParam Integer orderId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        Order order = orderMapper.findById(orderId);
        if (order == null || !order.getUserId().equals(userId)) {
            return Result.success(false);
        }
        return Result.success(merchantRatingMapper.countByOrderId(orderId) > 0);
    }
}
