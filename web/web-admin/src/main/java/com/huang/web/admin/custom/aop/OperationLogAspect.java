package com.huang.web.admin.custom.aop;

import com.huang.common.result.Result;
import com.huang.web.admin.service.biz.AdminOperationLogBizService;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class OperationLogAspect {

    private static final int SUCCESS_CODE = 200;

    private final AdminOperationLogBizService adminOperationLogBizService;

    public OperationLogAspect(AdminOperationLogBizService adminOperationLogBizService) {
        this.adminOperationLogBizService = adminOperationLogBizService;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        boolean success = false;
        String detail = buildDetail(operationLog, joinPoint.getArgs());
        try {
            Object result = joinPoint.proceed();
            success = isSuccess(result);
            return result;
        } catch (Throwable ex) {
            detail = trim(detail + " | ex=" + ex.getClass().getSimpleName() + ":" + ex.getMessage(), 500);
            throw ex;
        } finally {
            adminOperationLogBizService.record(operationLog.module(), operationLog.action(), detail, success);
        }
    }

    private boolean isSuccess(Object result) {
        if (result instanceof Result<?> r) {
            return r.getCode() != null && r.getCode() == SUCCESS_CODE;
        }
        return result != null;
    }

    private String buildDetail(OperationLog operationLog, Object[] args) {
        String argText = Arrays.toString(args);
        String detail = operationLog.detail();
        if (detail == null || detail.isBlank()) {
            detail = "args=" + argText;
        } else {
            detail = detail + " | args=" + argText;
        }
        return trim(detail, 500);
    }

    private String trim(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}

