package com.wangheng.mapper;

import com.wangheng.pojo.UserCoupon;
import com.wangheng.pojo.UserCouponVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户优惠券表 Mapper。
 * 带类型摘要的查询（listWithType）在 UserCouponMapper.xml。
 */
@Mapper
public interface UserCouponMapper {

    /** 我的优惠券（联类型摘要，状态筛选） */
    List<UserCouponVO> listWithType(@Param("userId") Integer userId, @Param("status") Integer status);

    @Insert("insert into user_coupon(user_id,coupon_type_id,coupon_stock_id,status,start_time,end_time,use_order_id) " +
            "values(#{userId},#{couponTypeId},#{couponStockId},#{status},#{startTime},#{endTime},#{useOrderId})")
    void insert(UserCoupon userCoupon);

    @Select("select * from user_coupon where id=#{id}")
    UserCoupon findById(Integer id);

    @Select("select count(*) from user_coupon where user_id=#{userId} and coupon_stock_id=#{stockId}")
    int countByUserAndStock(@Param("userId") Integer userId, @Param("stockId") Integer stockId);

    /** 我的该活动未使用的优惠券（取消抢购用） */
    @Select("select * from user_coupon where user_id=#{userId} and coupon_stock_id=#{stockId} and status=0")
    UserCoupon findActiveByUserAndStock(@Param("userId") Integer userId, @Param("stockId") Integer stockId);

    /** 取消抢购：未使用 → 已过期(2)，只能操作自己的券 */
    @Update("update user_coupon set status=2 where id=#{id} and user_id=#{userId} and status=0")
    int cancel(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 批量过期（定时任务）：未使用且已过期的券 */
    @Update("update user_coupon set status=2 where status=0 and end_time &lt; now()")
    int markExpired();

    /** 核销（下单）：条件更新防并发重复使用 */
    @Update("update user_coupon set status=1,use_order_id=#{orderId} " +
            "where id=#{id} and status=0 and user_id=#{userId}")
    int markUsed(@Param("id") Integer id, @Param("orderId") Integer orderId, @Param("userId") Integer userId);

    /** 释放（取消订单）：已使用 → 未使用 */
    @Update("update user_coupon set status=0,use_order_id=null where id=#{id} and status=1")
    int restore(Integer id);

    /** 本店已发放（领取）张数 */
    @Select("select count(*) from user_coupon uc join coupon_stock cs on uc.coupon_stock_id = cs.id " +
            "where cs.create_user_id=#{userId}")
    int countGranted(Integer userId);

    /** 本店已使用张数 */
    @Select("select count(*) from user_coupon uc join coupon_stock cs on uc.coupon_stock_id = cs.id " +
            "where cs.create_user_id=#{userId} and uc.status=1")
    int countUsed(Integer userId);
}
