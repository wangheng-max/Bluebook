package com.wangheng.merchant.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 商家认证状态返回对象（GET /merchant/status）。
 * 前端据此决定是否显示"商家中心"入口。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MerchantStatusVO {
    private Integer userId;
    /** 0=待审核 1=已通过 2=已拒绝 3=已封禁 4=未申请 */
    private Integer merchantStatus;
    private String statusDesc;
    private String auditRemark;
    private String shopName;
    private String shopLogo;
    /** 申请时间（merchant_info.create_time） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime applyTime;
    /** 最近一次审核/变更时间：表无 audit_time 列，用 update_time 表示（见 Bug 清单 B-001） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;
}
