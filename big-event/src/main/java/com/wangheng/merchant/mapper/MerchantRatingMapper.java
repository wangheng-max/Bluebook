package com.wangheng.merchant.mapper;

import com.wangheng.merchant.pojo.MerchantRatingVO;






import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商家评分表 Mapper（表 merchant_rating，一单一评，uk_order 唯一键兜底）。
 */
@Mapper
public interface MerchantRatingMapper {

    @Insert("insert into merchant_rating(merchant_user_id,order_id,user_id,score,content,create_time) " +
            "values(#{merchantUserId},#{orderId},#{userId},#{score},#{content},now())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(@Param("merchantUserId") Integer merchantUserId,
                @Param("orderId") Integer orderId,
                @Param("userId") Integer userId,
                @Param("score") Integer score,
                @Param("content") String content);

    /** 该订单是否已评价（一单一评） */
    @Select("select count(*) from merchant_rating where order_id=#{orderId}")
    int countByOrderId(Integer orderId);

    /** 店铺平均分（无评分时 null） */
    @Select("select avg(score) from merchant_rating where merchant_user_id=#{merchantUserId}")
    Double avgScore(Integer merchantUserId);

    /** 店铺评分总数 */
    @Select("select count(*) from merchant_rating where merchant_user_id=#{merchantUserId}")
    int countByMerchant(Integer merchantUserId);

    /** 店铺评价列表（带评价人昵称/头像，按时间倒序；PageHelper 分页） */
    @Select("select r.id, r.score, r.content, u.username, u.user_pic as userPic, r.create_time as createTime " +
            "from merchant_rating r left join user u on u.id = r.user_id " +
            "where r.merchant_user_id=#{merchantUserId} order by r.create_time desc, r.id desc")
    List<MerchantRatingVO> listByMerchant(Integer merchantUserId);
}
