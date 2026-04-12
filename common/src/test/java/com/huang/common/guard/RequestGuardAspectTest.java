package com.huang.common.guard;

import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestGuardAspectTest {

    @Mock
    private RedisGuardSupport redisGuardSupport;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private RequestGuardAspect requestGuardAspect;

    @BeforeEach
    void setUp() {
        requestGuardAspect = new RequestGuardAspect(redisGuardSupport);
        LoginUserHolder.setLoginUser(new LoginUser(12L, "member_chen", Set.of("MEMBER")));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/app/course/enroll");
        request.addHeader("X-Forwarded-For", "127.0.0.1");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        LoginUserHolder.clear();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void applyGuards_shouldResolveSpelAndProceedWhenAllowed() throws Throwable {
        DemoDTO dto = new DemoDTO();
        dto.setScheduleId(33L);
        Method method = DemoController.class.getDeclaredMethod("create", DemoDTO.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        when(joinPoint.proceed()).thenReturn(Result.ok("ok"));
        when(redisGuardSupport.fixedWindowDecision("rate:12:33", 2, 30))
                .thenReturn(RedisGuardSupport.GuardDecision.ALLOW);
        when(redisGuardSupport.idempotentDecision("idem:12:33", 5))
                .thenReturn(RedisGuardSupport.GuardDecision.ALLOW);

        Object result = requestGuardAspect.applyGuards(joinPoint);

        assertThat(result).isInstanceOf(Result.class);
        verify(redisGuardSupport).fixedWindowDecision("rate:12:33", 2, 30);
        verify(redisGuardSupport).idempotentDecision("idem:12:33", 5);
        verify(joinPoint).proceed();
    }

    @Test
    void applyGuards_shouldFailOpenWhenExpressionCannotBeResolved() throws Throwable {
        Method method = DemoController.class.getDeclaredMethod("failOpen", String.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{"value"});
        when(joinPoint.proceed()).thenReturn(Result.ok("ok"));

        Object result = requestGuardAspect.applyGuards(joinPoint);

        assertThat(result).isInstanceOf(Result.class);
        verify(redisGuardSupport, never()).fixedWindowDecision(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt(),
                org.mockito.ArgumentMatchers.anyLong()
        );
        verify(joinPoint).proceed();
    }

    @Test
    void applyGuards_shouldAllowRateLimitWhenRedisDegraded() throws Throwable {
        DemoDTO dto = new DemoDTO();
        dto.setScheduleId(33L);
        Method method = DemoController.class.getDeclaredMethod("createRateOnly", DemoDTO.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        when(joinPoint.proceed()).thenReturn(Result.ok("ok"));
        when(redisGuardSupport.fixedWindowDecision("rate:12:33", 2, 30))
                .thenReturn(RedisGuardSupport.GuardDecision.DEGRADED);

        Object result = requestGuardAspect.applyGuards(joinPoint);

        assertThat(result).isInstanceOf(Result.class);
        verify(joinPoint).proceed();
    }

    @Test
    void applyGuards_shouldBlockIdempotentWhenRedisDegraded() throws Throwable {
        DemoDTO dto = new DemoDTO();
        dto.setScheduleId(33L);
        Method method = DemoController.class.getDeclaredMethod("create", DemoDTO.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[]{dto});
        when(redisGuardSupport.fixedWindowDecision("rate:12:33", 2, 30))
                .thenReturn(RedisGuardSupport.GuardDecision.ALLOW);
        when(redisGuardSupport.idempotentDecision("idem:12:33", 5))
                .thenReturn(RedisGuardSupport.GuardDecision.DEGRADED);

        Object result = requestGuardAspect.applyGuards(joinPoint);

        assertThat(result).isInstanceOf(Result.class);
        Result<?> response = (Result<?>) result;
        assertThat(response.getCode()).isEqualTo(203);
        assertThat(response.getMessage()).isEqualTo("系统繁忙，请稍后重试");
        verify(joinPoint, never()).proceed();
    }

    @SuppressWarnings("unused")
    private static class DemoController {

        @RateLimit(prefix = "rate:", key = "#userId + ':' + #dto.scheduleId", maxRequests = 2, windowSec = 30, message = "blocked")
        @IdempotentSubmit(prefix = "idem:", key = "#userId + ':' + #dto.scheduleId", ttlSec = 5, message = "dup")
        public Result<String> create(DemoDTO dto) {
            return Result.ok("ok");
        }

        @RateLimit(prefix = "rate:", key = "#missing.value", maxRequests = 1, windowSec = 10, message = "blocked", failOpen = true)
        public Result<String> failOpen(String body) {
            return Result.ok(body);
        }

        @RateLimit(prefix = "rate:", key = "#userId + ':' + #dto.scheduleId", maxRequests = 2, windowSec = 30, message = "blocked")
        public Result<String> createRateOnly(DemoDTO dto) {
            return Result.ok("ok");
        }
    }

    private static class DemoDTO {
        private Long scheduleId;

        public Long getScheduleId() {
            return scheduleId;
        }

        public void setScheduleId(Long scheduleId) {
            this.scheduleId = scheduleId;
        }
    }
}
