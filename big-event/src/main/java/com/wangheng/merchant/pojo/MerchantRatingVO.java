package com.wangheng.merchant.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

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
    /** 评价关联订单的第一个商品名（便于展示"评价了什么"） */
    private String productName;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
