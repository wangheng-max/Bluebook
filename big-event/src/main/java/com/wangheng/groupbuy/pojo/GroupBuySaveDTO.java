package com.wangheng.groupbuy.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;



/**
 * 团购活动创建/修改参数（POST /group-buy、PUT /group-buy/{id}，商家接口）。
 * id 在修改时必传，创建时忽略。
 */
@Data
public class GroupBuySaveDTO {
    private Integer id;
    @NotNull(message = "商品不能为空")
    private Integer productId;
    @NotEmpty(message = "团购标题不能为空")
    private String title;
    private String description;
    @NotNull(message = "团购价不能为空")
    @DecimalMin(value = "0.01", message = "团购价必须大于 0")
    private BigDecimal groupPrice;
    @NotNull(message = "成团人数门槛不能为空")
    @Min(value = 1, message = "成团人数门槛至少为 1")
    private Integer groupSize;
    @Min(value = 1, message = "最大团购人数至少为 1")
    private Integer maxGroupSize;
    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    @NotNull(message = "结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
}
