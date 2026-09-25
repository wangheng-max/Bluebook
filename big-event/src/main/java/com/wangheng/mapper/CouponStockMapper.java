package com.wangheng.mapper;

import com.wangheng.pojo.CouponStock;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 优惠券库存（发放活动）表 Mapper。
 * 动态查询（list/listOwn）在 CouponStockMapper.xml。
 */
@Mapper
public interface CouponStockMapper {

    /** 发放活动列表（公开，状态筛选） */
    List<CouponStock> list(@Param("status") Integer status);

    /** 商家自己的发放活动列表 */
    List<CouponStock> listOwn(@Param("userId") Integer userId, @Param("status") Integer status);

    @Select("select * from coupon_stock where id=#{id}")
    CouponStock findById(Integer id);

    @Insert("insert into coupon_stock(coupon_type_id,total_count,remain_count,used_count,start_time,end_time,status," +
            "create_user_id,create_time,update_time) " +
            "values(#{couponTypeId},#{totalCount},#{remainCount},0,#{startTime},#{endTime},#{status}," +
            "#{createUserId},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CouponStock stock);

    @Update("update coupon_stock set remain_count=#{remainCount} where id=#{id}")
    int updateRemainCount(@Param("id") Integer id, @Param("remainCount") Integer remainCount);

    @Update("update coupon_stock set status=#{status} where id=#{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("update coupon_stock set used_count=used_count+1 where id=#{id}")
    int incrUsedCount(Integer id);

    @Select("select count(*) from coupon_stock where id=#{id} and create_user_id=#{userId}")
    int countOwned(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("select count(*) from coupon_stock where create_user_id=#{userId}")
    int countByUser(Integer userId);
}
