package com.wangheng.groupbuy.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 我的团购进度返回对象（GET /group-buy/my-progress）
 */
@Data
public class GroupBuyProgressVO {
    private Integer id;
    private String title;
    private Integer groupSize;
    private Integer progressCount;
    private BigDecimal groupPrice;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    /** 0=未开始 1=进行中 2=已成团结束 3=已关闭 */
    private Integer status;
    /** 我的参团份数 */
    private Integer myGroupNum;
}
