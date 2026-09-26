package com.wangheng.groupbuy.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团购详情返回对象（GET /group-buy/{id}）：团购信息 + 商品摘要 + 我的参团情况
 */
@Data
public class GroupBuyDetailVO {
    private Integer id;
    private Integer productId;
    private String productName;
    private String productCover;
    private BigDecimal originPrice;
    private String title;
    private String description;
    private BigDecimal groupPrice;
    private Integer groupSize;
    private Integer progressCount;
    private Integer maxGroupSize;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    /** 0=未开始 1=进行中 2=已成团结束 3=已关闭 */
    private Integer status;
    /** 我是否已参团 */
    private Boolean joined;
    /** 我的参团份数 */
    private Integer myGroupNum;
}
