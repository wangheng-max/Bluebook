package com.wangheng.merchant.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 商家评分展示对象（店铺页评价列表）。
 */
@Data
public class MerchantRatingVO {
    private Integer id;
    private Integer score;
    private String content;
    private String username;
    private String userPic;
    /** 评价关联订单的第一个商品ID（前端跳商品详情页） */
    private Integer productId;
    /** 评价关联订单的第一个商品图 */
    private String productImg;
    /** 评价关联订单所购商品名（多件逗号分隔，便于展示"评价了什么"） */
    private String productName;
    /** 该评价下的评论数（target_type=rating 的评论） */
    private Integer commentCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
