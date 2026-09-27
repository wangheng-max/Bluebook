package com.wangheng.coupon.pojo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;



/**
 * 优惠券类型创建/修改参数（POST /merchant/coupon/type、PUT /merchant/coupon/type/{id}，商家接口）。
 * id 在修改时必传；满减券填 discountAmount，折扣券填 discountRate（服务层校验二选一）。
 */
@Data
public class CouponTypeDTO {
    private Integer id;
    @NotEmpty(message = "券类型名称不能为空")
    private String name;
    /** 满减券最低消费 */
    private BigDecimal minSpend;
    /** 满减券减免金额 */
    private BigDecimal discountAmount;
    /** 折扣券折扣率，0.85=85折 */
    private BigDecimal discountRate;
    @NotNull(message = "领取后有效期不能为空")
    @Min(value = 1, message = "有效期至少 1 天")
    private Integer validDays;
    /** 最大使用次数，0=不限 */
    private Integer maxUses;
}
