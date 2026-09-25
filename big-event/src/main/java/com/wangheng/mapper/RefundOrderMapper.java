package com.wangheng.mapper;

import com.wangheng.pojo.RefundOrder;
import com.wangheng.pojo.RefundOrderVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 退款单表 Mapper。
 * 联订单摘要查询（myList/listMerchant）在 RefundOrderMapper.xml。
 */
@Mapper
public interface RefundOrderMapper {

    /** 我的退款单（联订单摘要） */
    List<RefundOrderVO> myList(@Param("userId") Integer userId);

    /** 商家退款单（本店商品订单的退款，数据隔离） */
    List<RefundOrderVO> listMerchant(@Param("userId") Integer userId);

    @Insert("insert into refund_order(order_id,refund_amount,refund_reason,refund_status,create_time,update_time) " +
            "values(#{orderId},#{refundAmount},#{refundReason},0,now(),now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(RefundOrder refundOrder);

    @Select("select * from refund_order where id=#{id}")
    RefundOrder findById(Integer id);

    /** 订单待处理退款单数量（防重复申请） */
    @Select("select count(*) from refund_order where order_id=#{orderId} and refund_status=0")
    int countPendingByOrder(Integer orderId);

    /** 审核：仅待处理(0)可审，0→1通过/0→2拒绝（条件更新保证幂等） */
    @Update("update refund_order set refund_status=#{status},audit_time=now(),remark=#{remark},update_time=now() " +
            "where id=#{id} and refund_status=0")
    int audit(@Param("id") Integer id, @Param("status") Integer status, @Param("remark") String remark);

    /** 记录退款完成时间 */
    @Update("update refund_order set refund_time=now() where id=#{id} and refund_status=3")
    int markRefundedTime(Integer id);

    /** 自动退款成功（团购未成团）：直接置 3 并记录时间 */
    @Update("update refund_order set refund_status=3,refund_time=now(),remark=#{remark},update_time=now() " +
            "where id=#{id}")
    int markAutoRefunded(@Param("id") Integer id, @Param("remark") String remark);

    /** 订单最新一条退款单状态（OrderVO.refundStatus，无退款为 null） */
    @Select("select refund_status from refund_order where order_id=#{orderId} order by id desc limit 1")
    Integer findLatestStatusByOrder(Integer orderId);
}
