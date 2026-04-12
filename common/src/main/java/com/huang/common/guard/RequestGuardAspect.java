package com.huang.common.guard;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.RequestIpUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Collection;

@Slf4j
@Aspect
@Component
public class RequestGuardAspect {

    private static final String SYSTEM_BUSY_MESSAGE = "\u7cfb\u7edf\u7e41\u5fd9\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5";

    private final RedisGuardSupport redisGuardSupport;
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    public RequestGuardAspect(RedisGuardSupport redisGuardSupport) {
        this.redisGuardSupport = redisGuardSupport;
    }

    @Around("execution(public * com.huang..controller..*(..))")
    public Object applyGuards(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        Collection<RateLimit> rateLimits = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, RateLimit.class);
        Collection<IdempotentSubmit> idempotentSubmits =
                AnnotatedElementUtils.getMergedRepeatableAnnotations(method, IdempotentSubmit.class);
        if (CollectionUtils.isEmpty(rateLimits) && CollectionUtils.isEmpty(idempotentSubmits)) {
            return joinPoint.proceed();
        }

        StandardEvaluationContext context = buildEvaluationContext(method, joinPoint.getArgs());

        if (!CollectionUtils.isEmpty(rateLimits)) {
            for (RateLimit rateLimit : rateLimits) {
                String key = evaluateGuardKey(rateLimit.key(), context, rateLimit.failOpen(), method);
                if (key == null) {
                    continue;
                }
                RedisGuardSupport.GuardDecision decision = redisGuardSupport.fixedWindowDecision(
                        rateLimit.prefix() + key,
                        rateLimit.maxRequests(),
                        rateLimit.windowSec()
                );
                if (decision == RedisGuardSupport.GuardDecision.BLOCK) {
                    return Result.fail(rateLimit.message());
                }
                if (decision == RedisGuardSupport.GuardDecision.DEGRADED) {
                    if (rateLimit.failOpen()) {
                        log.warn("rate limit guard degraded to fail-open, method={}, key={}",
                                method.getName(), rateLimit.prefix() + key);
                        continue;
                    }
                    return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), SYSTEM_BUSY_MESSAGE);
                }
            }
        }

        if (!CollectionUtils.isEmpty(idempotentSubmits)) {
            for (IdempotentSubmit idempotentSubmit : idempotentSubmits) {
                String key = evaluateGuardKey(idempotentSubmit.key(), context, idempotentSubmit.failOpen(), method);
                if (key == null) {
                    continue;
                }
                RedisGuardSupport.GuardDecision decision = redisGuardSupport.idempotentDecision(
                        idempotentSubmit.prefix() + key,
                        idempotentSubmit.ttlSec()
                );
                if (decision == RedisGuardSupport.GuardDecision.BLOCK) {
                    return Result.fail(idempotentSubmit.message());
                }
                if (decision == RedisGuardSupport.GuardDecision.DEGRADED) {
                    log.warn("idempotent guard degraded to fail-close, method={}, key={}",
                            method.getName(), idempotentSubmit.prefix() + key);
                    return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), SYSTEM_BUSY_MESSAGE);
                }
            }
        }

        return joinPoint.proceed();
    }

    private StandardEvaluationContext buildEvaluationContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length && i < args.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        context.setVariable("userId", loginUser == null ? null : loginUser.getUserId());
        context.setVariable("ip", RequestIpUtil.resolveClientIp());
        context.setVariable("requestUri", resolveRequestUri());
        return context;
    }

    private String evaluateGuardKey(String expression, StandardEvaluationContext context, boolean failOpen, Method method) {
        if (!StringUtils.hasText(expression)) {
            return normalize(resolveRequestUri());
        }
        try {
            Object value = expressionParser.parseExpression(expression).getValue(context);
            if (value == null) {
                return normalize(resolveRequestUri());
            }
            return normalize(String.valueOf(value));
        } catch (Exception ex) {
            log.warn("guard key evaluate failed, method={}, expression={}", method.getName(), expression, ex);
            if (failOpen) {
                return null;
            }
            return normalize(resolveRequestUri());
        }
    }

    private String resolveRequestUri() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }
        HttpServletRequest request = attributes.getRequest();
        return request == null ? "unknown" : request.getRequestURI();
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "unknown";
        }
        return value.trim().replace(' ', '_');
    }
}
