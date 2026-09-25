package com.wangheng.pojo;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

/**
 * 店铺信息修改参数（PUT /merchant/info）：店名、头像、描述
 */
@Data
public class ShopInfoDTO {
    @NotEmpty(message = "店铺名称不能为空")
    private String shopName;
    private String shopLogo;
    private String shopDescription;
}
