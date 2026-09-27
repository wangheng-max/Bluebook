package com.wangheng.coupon.mapper;

import com.wangheng.coupon.pojo.CouponType;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 优惠券类型表 Mapper（商家创建，数据隔离 create_user_id）
 */
@Mapper
public interface CouponTypeMapper {

    @Insert("insert into coupon_type(name,min_spend,discount_amount,discount_rate,gift_product_id,max_uses,valid_days," +
            "create_user_id,create_time) " +
            "values(#{name},#{minSpend},#{discountAmount},#{discountRate},#{giftProductId},#{maxUses},#{validDays}," +
            "#{createUserId},now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CouponType couponType);

    @Select("select * from coupon_type where id=#{id}")
    CouponType findById(Integer id);

    /** 修改：WHERE 同时校验归属 */
    @Update("update coupon_type set name=#{name},min_spend=#{minSpend},discount_amount=#{discountAmount}," +
            "discount_rate=#{discountRate},valid_days=#{validDays},max_uses=#{maxUses} " +
            "where id=#{id} and create_user_id=#{createUserId}")
    int update(CouponType couponType);

    @Select("select * from coupon_type order by id desc")
    List<CouponType> findAll();

    @Select("select count(*) from coupon_type where id=#{id} and create_user_id=#{userId}")
    int countOwned(@Param("id") Integer id, @Param("userId") Integer userId);

    @Select("select count(*) from coupon_type where create_user_id=#{userId}")
    int countByUser(Integer userId);

    /** 正在被进行中/未开始发放活动使用的数量（>0 则不允许修改类型） */
    @Select("select count(*) from coupon_stock where coupon_type_id=#{typeId} and status in (0,1)")
    int countActiveStockByType(Integer typeId);
}
