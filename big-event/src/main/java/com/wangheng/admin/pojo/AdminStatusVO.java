package com.wangheng.admin.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户的管理员身份状态（平台管理菜单可见性判断）。
 * 只返回调用者自身是否为管理员，不包含任何管理员账号信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatusVO {
    private Boolean admin;//是否管理员（user_role role_type=3 且生效）
}
