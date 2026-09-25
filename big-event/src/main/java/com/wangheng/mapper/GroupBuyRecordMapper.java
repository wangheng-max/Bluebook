package com.wangheng.mapper;

import com.wangheng.pojo.GroupBuyRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 团购记录表 Mapper。
 * uk_user_group(user_id, group_buy_id) 防重复参团（业务层先查后插 + 唯一键兜底）。
 */
@Mapper
public interface GroupBuyRecordMapper {

    @Insert("insert into group_buy_record(group_buy_id,order_id,user_id,group_num,join_time) " +
            "values(#{groupBuyId},#{orderId},#{userId},#{groupNum},now())")
    void insert(GroupBuyRecord record);

    /** 活动参与记录数（用于团购进度） */
    @Select("select count(*) from group_buy_record where group_buy_id=#{groupBuyId}")
    int countByGroupBuyId(Integer groupBuyId);

    /** 活动全部参与记录（定时任务结算用） */
    @Select("select * from group_buy_record where group_buy_id=#{groupBuyId}")
    List<GroupBuyRecord> findByGroupBuyId(Integer groupBuyId);

    /** 我的参团份数 */
    @Select("select coalesce(sum(group_num),0) from group_buy_record where group_buy_id=#{groupBuyId} and user_id=#{userId}")
    Integer sumGroupNum(@Param("userId") Integer userId, @Param("groupBuyId") Integer groupBuyId);

    /** 我是否已参团 */
    @Select("select count(*) from group_buy_record where group_buy_id=#{groupBuyId} and user_id=#{userId}")
    int countByUserAndGroupBuy(@Param("userId") Integer userId, @Param("groupBuyId") Integer groupBuyId);
}
