package com.wangheng.pojo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 管理员审核参数（POST /admin/merchant/{userId}/audit）
 */
@Data
public class AuditRequestDTO {
    /** true=通过 false=拒绝 */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;
    private String auditRemark;
}
