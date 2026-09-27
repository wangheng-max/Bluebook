package com.wangheng.order.mapper;

import com.wangheng.order.pojo.Order;
import com.wangheng.order.pojo.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;









/**
 * 订单表 Mapper（表 t_order）。
 * 动态查询（myList/listMerchant/findTimeoutOrders）在 OrderMapper.xml。
 * 状态流转全部用"条件更新"（WHERE 带旧状态），保证状态机幂等。
 */
@Mapper
public interface OrderMapper {

    /** 我的订单（状态筛选） */
    List<Order> myList(@Param("userId") Integer userId, @Param("status") Integer status);

    /** 商家订单（含本店商品的订单，数据隔离） */
    List<Order> listMerchant(@Param("userId") Integer userId, @Param("status") Integer status);

    /** 超时未支付订单（定时任务） */
    List<Order> findTimeoutOrders(@Param("deadline") LocalDateTime deadline);

    @Select("select * from t_order where id=#{id}")
    Order findById(Integer id);

    /** 用户在同一团购下的有效订单数（待支付0/已支付1），防重复参团下单占库存 */
    @Select("select count(*) from t_order where user_id=#{userId} and group_buy_id=#{groupBuyId} and status in (0,1)")
    int countUserGroupOrders(@Param("userId") Integer userId, @Param("groupBuyId") Integer groupBuyId);

    /** 校验订单：本人 + 已支付及之后(1/2/3) + 包含该商品（商品评论晒单用） */
    @Select("select count(*) from t_order o join order_item oi on oi.order_id = o.id " +
            "where o.id = #{orderId} and o.user_id = #{userId} and oi.product_id = #{productId} and o.status in (1,2,3)")
    int countOwnPaidOrderWithProduct(@Param("orderId") Integer orderId, @Param("userId") Integer userId,
                                     @Param("productId") Integer productId);

    /** 我购买某商品的有效订单明细（状态 1/2/3，新的在前；评论晒单选择用） */
    @Select("select oi.* from order_item oi join t_order o on o.id = oi.order_id " +
            "where o.user_id = #{userId} and oi.product_id = #{productId} and o.status in (1,2,3) " +
            "order by o.id desc")
    List<OrderItem> findPurchasedItems(@Param("userId") Integer userId, @Param("productId") Integer productId);

    /** 订单归属商家（取第一个明细商品的上架者；本项目单店下单，订单维度评价用） */
    @Select("select p.create_user_id from order_item oi join product p on p.id = oi.product_id " +
            "where oi.order_id = #{orderId} and p.create_user_id is not null limit 1")
    Integer findMerchantUserIdByOrderId(Integer orderId);

    @Insert("insert into t_order(order_no,order_type,user_id,group_buy_id,address_id,total_amount,pay_amount," +
            "user_coupon_id,status,remark,create_time,update_time) " +
            "values(#{orderNo},#{orderType},#{userId},#{groupBuyId},#{addressId},#{totalAmount},#{payAmount}," +
            "#{userCouponId},0,#{remark},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(Order order);

    /** 支付：0→1 */
    @Update("update t_order set status=1,pay_time=now(),update_time=now() where id=#{id} and status=0")
    int markPaid(Integer id);

    /** 取消：0→4 */
    @Update("update t_order set status=4,cancel_time=now(),cancel_reason=#{reason},update_time=now() " +
            "where id=#{id} and status=0")
    int markCancelled(@Param("id") Integer id, @Param("reason") String reason);

    /** 发货：1→2 */
    @Update("update t_order set status=2,ship_time=now(),update_time=now() where id=#{id} and status=1")
    int markShipped(Integer id);

    /** 申请退款：1/2→5 */
    @Update("update t_order set status=5,update_time=now() where id=#{id} and status in (1,2)")
    int markRefunding(Integer id);

    /** 退款完成：5→6 */
    @Update("update t_order set status=6,update_time=now() where id=#{id} and status=5")
    int markRefunded(Integer id);

    /** 团购未成团自动退款：1→6 */
    @Update("update t_order set status=6,update_time=now() where id=#{id} and status=1")
    int markGroupRefunded(Integer id);

    /** 退款被拒绝：恢复原状态（1 或 2） */
    @Update("update t_order set status=#{status},update_time=now() where id=#{id} and status=5")
    int restoreFromRefunding(@Param("id") Integer id, @Param("status") Integer status);
}
