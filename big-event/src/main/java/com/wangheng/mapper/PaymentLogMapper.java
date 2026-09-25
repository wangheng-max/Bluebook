package com.wangheng.mapper;

import com.wangheng.pojo.PaymentLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付流水表 Mapper（本期模拟支付）
 */
@Mapper
public interface PaymentLogMapper {

    @Insert("insert into payment_log(order_id,pay_type,pay_amount,pay_status,create_time) " +
            "values(#{orderId},0,#{payAmount},0,now())")
    void insert(PaymentLog log);

    /** 模拟支付成功：0→1，记录支付方式与流水号 */
    @Update("update payment_log set pay_type=#{payType},pay_status=1,pay_time=now()," +
            "third_party_trade_no=#{tradeNo} where order_id=#{orderId} and pay_status=0")
    int markPaid(@Param("orderId") Integer orderId, @Param("payType") Integer payType,
                 @Param("tradeNo") String tradeNo);
}
