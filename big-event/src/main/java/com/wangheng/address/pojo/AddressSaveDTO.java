package com.wangheng.address.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


/**
 * 收货地址添加/修改参数（POST/PUT /user/address）。
 * id 在修改时必传，添加时忽略。
 */
@Data
public class AddressSaveDTO {
    private Integer id;
    @NotEmpty(message = "收货人姓名不能为空")
    private String receiverName;
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String receiverPhone;
    private String province;
    private String city;
    private String district;
    @NotEmpty(message = "详细地址不能为空")
    private String detailAddress;
    /** 是否设为默认地址 */
    private Boolean isDefault;
}
