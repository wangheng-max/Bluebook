package com.wangheng.merchant.pojo;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 商家认证申请参数（POST /merchant/apply、/merchant/apply/re-submit）
 */
@Data
public class MerchantApplyDTO {
    @NotEmpty(message = "店铺名称不能为空")
    private String shopName;
    private String shopDescription;
    private String shopLogo;
    @NotEmpty(message = "营业执照号不能为空")
    private String licenseNumber;
    @NotEmpty(message = "营业执照图片不能为空")
    private String licenseImg;
    @NotEmpty(message = "联系人姓名不能为空")
    private String contactName;
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String address;
}
