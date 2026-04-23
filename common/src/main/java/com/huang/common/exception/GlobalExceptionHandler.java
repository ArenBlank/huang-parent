package com.huang.common.exception;

import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentNotValidException.class,
            BindException.class,
            MissingServletRequestParameterException.class,
            HttpRequestMethodNotSupportedException.class
    })
    public Result<?> handleBadRequest(Exception e) {
        log.warn("request param invalid: {}", e.getMessage(), e);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), resolveBadRequestMessage(e));
    }

    @ExceptionHandler(HuangException.class)
    public Result<?> handle(HuangException e) {
        String message = e.getMessage();
        Integer code = e.getCode();
        log.warn("business exception, code={}, message={}", code, message, e);
        return Result.fail(code, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<?> handle(Exception e) {
        log.error("unexpected exception", e);
        return Result.fail();
    }

    private String resolveBadRequestMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException ex && ex.getBindingResult().getFieldError() != null) {
            return ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        if (e instanceof BindException ex && ex.getBindingResult().getFieldError() != null) {
            return ex.getBindingResult().getFieldError().getDefaultMessage();
        }
        if (e instanceof MissingServletRequestParameterException ex) {
            return "缺少必要参数: " + ex.getParameterName();
        }
        if (e instanceof HttpRequestMethodNotSupportedException) {
            return "请求方式不支持，请确认当前后端服务已升级到最新接口后再重试";
        }
        if (e instanceof HttpMessageNotReadableException) {
            return "请求体格式错误";
        }
        return ResultCodeEnum.PARAM_ERROR.getMessage();
    }
}
