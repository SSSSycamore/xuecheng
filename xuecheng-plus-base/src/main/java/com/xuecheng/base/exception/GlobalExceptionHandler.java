package com.xuecheng.base.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpStatus;

import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(XueChengPlusException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse customException(XueChengPlusException e) {
        // 记录日志
        log.error("系统异常: {}", e.getMessage(), e);
        // 返回错误响应
        return new RestErrorResponse(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse validateException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        List<FieldError> fieldErrors = bindingResult.getFieldErrors();
        List<String> errStrs = new ArrayList<>();
        fieldErrors.stream().forEach(
                err->errStrs.add(err.getDefaultMessage())
        );
        // 拼接错误信息
        String errMsg = StringUtils.join(errStrs, ",");
        // 记录日志
        log.error("传入参数异常: {}", e.getMessage(), errMsg);
        // 返回错误响应
        return new RestErrorResponse(errMsg);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public RestErrorResponse Exception(Exception e) {
        // 记录日志
        log.error("系统异常: {}", e.getMessage(), e);
        // 返回错误响应
        return new RestErrorResponse(CommonError.UNKNOWN_ERROR.getErrMessage());
    }
}
