package com.wangheng.exception;

import com.wangheng.pojo.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e){
        e.printStackTrace();
        return Result.error(StringUtils.hasLength(e.getMessage())? e.getMessage() : "操作失败");
    }

    /**
     * 上传文件超过 spring.servlet.multipart 限制：不暴露异常堆栈，只给明确提示
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result handleMaxUploadSize(MaxUploadSizeExceededException e){
        return Result.error("上传文件过大，单个文件不能超过 50MB");
    }

    /**
     * 商家/管理员权限校验失败：返回 HTTP 403（商城商家认证机制 v1.1）
     */
    @ExceptionHandler(MerchantAuthException.class)
    public ResponseEntity<Result> handleMerchantAuthException(MerchantAuthException e){
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Result.error(e.getMessage()));
    }
}
