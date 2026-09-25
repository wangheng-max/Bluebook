package com.wangheng.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商家认证信息实体（表 merchant_info，v1.1 商家认证机制）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantInfo {
    private Integer id;
    private Integer userId;
    private String shopName;
    private String shopLogo;
    private String shopDescription;
    private String licenseNumber;
    private String licenseImg;
    private String contactName;
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String address;
    /** 0=待审核 1=审核通过 2=审核拒绝 3=已封禁 */
    private Integer merchantStatus;
    private String auditRemark;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
