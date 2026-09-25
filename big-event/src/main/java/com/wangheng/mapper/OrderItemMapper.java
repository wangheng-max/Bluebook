package com.wangheng.mapper;

import com.wangheng.pojo.OrderItem;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 订单明细表 Mapper。
 * uk_order_product(order_id, product_id, sku_id) 唯一键兜底重复明细。
 */
@Mapper
public interface OrderItemMapper {

    @Insert("insert into order_item(order_id,product_id,product_name,sku_id,sku_name,product_img,price,quantity," +
            "total_price,coupon_discount) " +
            "values(#{orderId},#{productId},#{productName},#{skuId},#{skuName},#{productImg},#{price},#{quantity}," +
            "#{totalPrice},#{couponDiscount})")
    void insert(OrderItem item);

    @Select("select * from order_item where order_id=#{orderId} order by id asc")
    List<OrderItem> findByOrderId(Integer orderId);

    /** 订单是否含本店商品（商家数据隔离） */
    @Select("select count(*) from order_item oi join product p on oi.product_id=p.id " +
            "where oi.order_id=#{orderId} and p.create_user_id=#{userId}")
    int countByOrderAndMerchant(@Param("orderId") Integer orderId, @Param("userId") Integer userId);
}
