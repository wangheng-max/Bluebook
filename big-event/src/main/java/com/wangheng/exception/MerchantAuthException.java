package com.wangheng.exception;

/**
 * 商家/管理员权限校验失败异常。
 * 由 GlobalExceptionHandler 统一捕获，返回 HTTP 403 + Result(1, 提示信息)。
 */
public class MerchantAuthException extends RuntimeException {

    public MerchantAuthException(String message) {
        super(message);
    }
}
