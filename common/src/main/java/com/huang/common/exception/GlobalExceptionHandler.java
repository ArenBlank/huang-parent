package com.huang.common.exception;

import com.huang.common.result.Result;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(Exception.class)
    public Result handle(Exception e) {
        e.printStackTrace();
        return Result.fail();
    }


    @ExceptionHandler(HuangException.class)
    public Result handle(HuangException e) {
        String message = e.getMessage();
        Integer code = e.getCode();
        e.printStackTrace();
        return Result.fail(code, message);
    }
}
