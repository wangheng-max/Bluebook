package com.wangheng.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 商家权限注解（v1.1 商家认证机制）。
 * 标注在商家经营接口（类或方法）上，由 RoleAuthAspect 切面校验：
 * user_role 商家角色生效(status=1) + merchant_info 认证通过(merchant_status=1)，
 * 校验失败抛 MerchantAuthException（HTTP 403）。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireMerchant {
}
