package com.wangheng.merchant.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 店铺主页信息（公开接口 GET /shop/{merchantUserId}）。
 */
@Data
public class ShopVO {
    private Integer userId;
    private String shopName;
    private String shopLogo;
    private String shopDescription;
    private String province;
    private String city;
    private String district;
    private String address;
    /** 平均评分（1 位小数，无评分时为 null） */
    private Double avgScore;
    /** 评分总数 */
    private Integer ratingCount;
    /** 店铺粉丝数（关注店铺的人数） */
    private Integer followerCount;
    /** 在架商品数 */
    private Integer productCount;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
