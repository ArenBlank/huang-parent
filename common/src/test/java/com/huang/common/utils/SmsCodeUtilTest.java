package com.huang.common.utils;

import com.huang.common.config.DevelopmentConfig;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisCacheSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmsCodeUtilTest {

    @Mock
    private DevelopmentConfig developmentConfig;

    @Mock
    private RedisCacheSupport redisCacheSupport;

    private SmsCodeUtil smsCodeUtil;

    @BeforeEach
    void setUp() {
        smsCodeUtil = new SmsCodeUtil(developmentConfig, redisCacheSupport);
    }

    @Test
    void sendSmsCode_shouldReturnFixedCodeInDevelopmentMode() {
        when(developmentConfig.isEnabled()).thenReturn(true);
        when(developmentConfig.getFixedSmsCode()).thenReturn("123456");

        String result = smsCodeUtil.sendSmsCode("13800138000", "login");

        assertThat(result).isEqualTo("123456");
        verify(redisCacheSupport, never()).setString(eq(RedisConstant.appSmsCodeKey("login", "13800138000")), eq("123456"), anyLong());
    }

    @Test
    void sendSmsCode_shouldCacheCodeAndCooldownOutsideDevelopmentMode() {
        when(developmentConfig.isEnabled()).thenReturn(false);

        String result = smsCodeUtil.sendSmsCode("13800138000", "login");

        assertThat(result).isNull();
        verify(redisCacheSupport).setString(
                eq(RedisConstant.appSmsCodeKey("login", "13800138000")),
                org.mockito.ArgumentMatchers.matches("\\d{6}"),
                eq((long) RedisConstant.APP_LOGIN_CODE_TTL_SEC)
        );
        verify(redisCacheSupport).setString(
                eq(RedisConstant.appSmsCooldownKey("login", "13800138000")),
                eq("1"),
                eq((long) RedisConstant.APP_LOGIN_CODE_RESEND_TIME_SEC)
        );
    }

    @Test
    void verifySmsCode_shouldUseFixedCodeWhenDevelopmentValidationIsSkipped() {
        when(developmentConfig.isEnabled()).thenReturn(true);
        when(developmentConfig.isSkipSmsValidation()).thenReturn(true);
        when(developmentConfig.getFixedSmsCode()).thenReturn("654321");

        boolean ok = smsCodeUtil.verifySmsCode("13800138000", "654321", "login");

        assertThat(ok).isTrue();
        verify(redisCacheSupport, never()).getString(RedisConstant.appSmsCodeKey("login", "13800138000"));
    }

    @Test
    void verifySmsCode_shouldDeleteCodeAndFailCounterOnSuccess() {
        when(developmentConfig.isEnabled()).thenReturn(false);
        when(redisCacheSupport.getString(RedisConstant.appSmsCodeKey("login", "13800138000"))).thenReturn("123456");

        boolean ok = smsCodeUtil.verifySmsCode("13800138000", "123456", "login");

        assertThat(ok).isTrue();
        verify(redisCacheSupport).safeDelete(RedisConstant.appSmsCodeKey("login", "13800138000"));
        verify(redisCacheSupport).safeDelete(RedisConstant.appSmsFailKey("login", "13800138000"));
    }

    @Test
    void canSendSms_shouldRespectCooldownKeyOutsideDevelopmentMode() {
        when(developmentConfig.isEnabled()).thenReturn(false);
        when(redisCacheSupport.getString(RedisConstant.appSmsCooldownKey("login", "13800138000"))).thenReturn("1");

        boolean canSend = smsCodeUtil.canSendSms("13800138000", "login");

        assertThat(canSend).isFalse();
    }
}
