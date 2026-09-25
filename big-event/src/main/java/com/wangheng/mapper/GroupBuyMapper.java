package com.wangheng.mapper;

import com.wangheng.pojo.GroupBuy;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 团购活动表 Mapper。
 * 动态查询（list/listOwn/listJoined）在 GroupBuyMapper.xml。
 */
@Mapper
public interface GroupBuyMapper {

    /** 团购活动列表（公开，状态筛选） */
    List<GroupBuy> list(@Param("status") Integer status);

    /** 商家自己的团购活动列表 */
    List<GroupBuy> listOwn(@Param("userId") Integer userId, @Param("status") Integer status);

    /** 我参与过的团购活动列表 */
    List<GroupBuy> listJoined(@Param("userId") Integer userId);

    @Select("select * from group_buy where id=#{id}")
    GroupBuy findById(Integer id);

    @Insert("insert into group_buy(product_id,title,description,group_price,group_size,progress_count,max_group_size," +
            "start_time,end_time,status,create_user_id,create_time,update_time) " +
            "values(#{productId},#{title},#{description},#{groupPrice},#{groupSize},0,#{maxGroupSize}," +
            "#{startTime},#{endTime},#{status},#{createUserId},now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(GroupBuy groupBuy);

    /** 修改活动（仅未开始可改）：WHERE 同时校验归属 */
    @Update("update group_buy set title=#{title},description=#{description},group_price=#{groupPrice}," +
            "group_size=#{groupSize},max_group_size=#{maxGroupSize},start_time=#{startTime},end_time=#{endTime} " +
            "where id=#{id} and create_user_id=#{createUserId}")
    int update(GroupBuy groupBuy);

    @Update("update group_buy set status=#{status} where id=#{id}")
    int updateStatus(@Param("id") Integer id, @Param("status") Integer status);

    @Update("update group_buy set progress_count=#{progressCount} where id=#{id}")
    int updateProgress(@Param("id") Integer id, @Param("progressCount") Integer progressCount);

    @Select("select count(*) from group_buy where id=#{id} and create_user_id=#{userId}")
    int countOwned(@Param("id") Integer id, @Param("userId") Integer userId);

    /** 同一商品进行中/未开始的团购数量（防止重复开团） */
    @Select("select count(*) from group_buy where product_id=#{productId} and status in (0,1)")
    int countActiveByProduct(Integer productId);
}
