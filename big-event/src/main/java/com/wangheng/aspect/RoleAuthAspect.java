package com.wangheng.aspect;

import com.wangheng.auth.MerchantPermissionChecker;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * 角色权限切面（v1.1 商家认证机制）。
 * @RequireMerchant：校验认证通过的商家；@RequireAdmin：校验管理员。
 * 类级注解对整个 Controller 生效，方法级注解只对单个方法生效。
 */
@Aspect
@Component
public class RoleAuthAspect {

    @Autowired
    private MerchantPermissionChecker merchantPermissionChecker;

    @Around("@annotation(com.wangheng.anno.RequireMerchant) || @within(com.wangheng.anno.RequireMerchant)")
    public Object checkMerchantAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        merchantPermissionChecker.checkMerchant();
        return joinPoint.proceed();
    }

    @Around("@annotation(com.wangheng.anno.RequireAdmin) || @within(com.wangheng.anno.RequireAdmin)")
    public Object checkAdminAuth(ProceedingJoinPoint joinPoint) throws Throwable {
        merchantPermissionChecker.checkAdmin();
        return joinPoint.proceed();
    }
}
