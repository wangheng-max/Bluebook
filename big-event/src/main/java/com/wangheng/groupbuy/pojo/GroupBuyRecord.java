package com.wangheng.groupbuy.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 团购记录实体（表 group_buy_record）。
 * uk_user_group(user_id, group_buy_id) 保证每人每活动仅一次记录，购买多份记在 groupNum。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupBuyRecord {
    private Integer id;
    private Integer groupBuyId;
    private Integer orderId;
    private Integer userId;
    /** 参与人数（购买份数） */
    private Integer groupNum;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;
}
