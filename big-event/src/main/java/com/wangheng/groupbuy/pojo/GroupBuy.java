package com.wangheng.groupbuy.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 团购活动实体（表 group_buy）。
 * status: 0=未开始 1=进行中 2=已成团结束 3=已关闭(未成团/手动关闭)。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuy {
    private Integer id;
    private Integer productId;
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
    private Integer status;
    private Integer createUserId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
