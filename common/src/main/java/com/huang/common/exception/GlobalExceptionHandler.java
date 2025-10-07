package com.huang.common.exception;

import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理请求体参数验证失败异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("参数验证失败: {}", message);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "参数验证失败: " + message);
    }

    /**
     * 处理表单参数验证失败异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e) {
        List<FieldError> fieldErrors = e.getBindingResult().getFieldErrors();
        String message = fieldErrors.stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        
        log.warn("表单参数验证失败: {}", message);
        return Result.fail(ResultCodeEnum.PARAM_ERROR.getCode(), "参数验证失败: " + message);
    }

    /**
     * 处理自定义异常
     */
    @ExceptionHandler(HuangException.class)
    public Result<Void> handleHuangException(HuangException e) {
        String message = e.getMessage();
        Integer code = e.getCode();
        log.error("业务异常: code={}, message={}", code, message, e);
        return Result.fail(code, message);
    }

    /**
     * 处理运行时异常
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常", e);
        return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), e.getMessage());
    }

    /**
     * 处理其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "系统内部错误，请联系管理员");
    }
}
