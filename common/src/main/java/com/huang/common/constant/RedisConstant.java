package com.huang.common.constant;

import java.util.Locale;

public class RedisConstant {
    private RedisConstant() {
    }

    public static final String CACHE_NULL_VALUE = "__NULL__";

    public static final String ADMIN_LOGIN_PREFIX = "admin:login:";
    public static final Integer ADMIN_LOGIN_CAPTCHA_TTL_SEC = 60;
    public static final String APP_LOGIN_PREFIX = "app:login:";
    public static final Integer APP_LOGIN_CODE_RESEND_TIME_SEC = 60;
    public static final Integer APP_LOGIN_CODE_TTL_SEC = 60 * 10;
    public static final String APP_ROOM_PREFIX = "app:room:";

    public static final long CACHE_NULL_TTL_SEC = 2 * 60;
    public static final long CACHE_LOCK_TTL_SEC = 10;
    public static final long IDEMPOTENT_TTL_SEC = 5;
    public static final long PAY_CALLBACK_GUARD_TTL_SEC = 10 * 60;
    public static final long LOGIN_RATE_LIMIT_WINDOW_SEC = 60;
    public static final long LOGIN_RATE_LIMIT_MAX = 10;
    public static final long SMS_SEND_RATE_LIMIT_WINDOW_SEC = 10 * 60;
    public static final long SMS_SEND_RATE_LIMIT_MAX = 5;
    public static final long SENSITIVE_WRITE_RATE_LIMIT_WINDOW_SEC = 30;
    public static final long SENSITIVE_WRITE_RATE_LIMIT_MAX = 5;

    public static final long APP_BANNER_TTL_SEC = 15 * 60;
    public static final long APP_NOTICE_TTL_SEC = 10 * 60;
    public static final long APP_SYS_CONFIG_TTL_SEC = 60 * 60;
    public static final long APP_COURSE_LIST_TTL_SEC = 10 * 60;
    public static final long APP_PLAN_LIST_TTL_SEC = 10 * 60;
    public static final long APP_PLAN_DETAIL_TTL_SEC = 15 * 60;
    public static final long LOCAL_CACHE_BANNER_TTL_SEC = 30;
    public static final long LOCAL_CACHE_NOTICE_TTL_SEC = 30;
    public static final long LOCAL_CACHE_SYS_CONFIG_TTL_SEC = 30;
    public static final long LOCAL_CACHE_COURSE_LIST_TTL_SEC = 15;
    public static final long LOCAL_CACHE_PLAN_LIST_TTL_SEC = 15;
    public static final long LOCAL_CACHE_PLAN_DETAIL_TTL_SEC = 30;
    public static final long TASK_LOCK_TTL_SEC = 30;

    public static final long JITTER_SHORT_SEC = 180;
    public static final long JITTER_LONG_SEC = 300;

    public static final String APP_BANNER_ACTIVE_KEY = "app:banner:active";
    public static final String APP_NOTICE_PUBLISHED_PREFIX = "app:notice:published:";
    public static final String APP_SYS_CONFIG_PREFIX = "app:sys-config:";
    public static final String APP_COURSE_LIST_PREFIX = "app:course:list:";
    public static final String APP_PLAN_LIST_ACTIVE_KEY = "app:plan:list:active";
    public static final String APP_PLAN_DETAIL_STATIC_PREFIX = "app:plan:detail:static:";
    public static final String APP_PLAN_DETAIL_LOCK_PREFIX = "app:plan:detail:lock:";
    public static final String CACHE_INVALIDATION_CHANNEL = "cache:evict";
    public static final String APP_SMS_CODE_PREFIX = "app:sms:code:";
    public static final String APP_SMS_COOLDOWN_PREFIX = "app:sms:cooldown:";
    public static final String APP_SMS_FAIL_PREFIX = "app:sms:fail:";
    public static final String APP_LOGIN_LIMIT_ACCOUNT_PREFIX = "app:limit:login:account:";
    public static final String APP_LOGIN_LIMIT_IP_PREFIX = "app:limit:login:ip:";
    public static final String ADMIN_LOGIN_LIMIT_ACCOUNT_PREFIX = "admin:limit:login:account:";
    public static final String ADMIN_LOGIN_LIMIT_IP_PREFIX = "admin:limit:login:ip:";
    public static final String APP_SMS_SEND_LIMIT_PREFIX = "app:limit:sms:send:";
    public static final String APP_COURSE_ENROLL_LIMIT_PREFIX = "app:limit:course:enroll:";
    public static final String APP_COURSE_ENROLL_IDEMPOTENT_PREFIX = "app:idem:course:enroll:";
    public static final String APP_BOOKING_CREATE_LIMIT_PREFIX = "app:limit:booking:create:";
    public static final String APP_BOOKING_CREATE_IDEMPOTENT_PREFIX = "app:idem:booking:create:";
    public static final String APP_PAY_CALLBACK_GUARD_PREFIX = "app:guard:pay:callback:";
    public static final String TASK_LOCK_PREFIX = "task:lock:";

    public static String appNoticePublishedKey(Integer limit) {
        return APP_NOTICE_PUBLISHED_PREFIX + limit;
    }

    public static String appSysConfigKey(String configKey) {
        return APP_SYS_CONFIG_PREFIX + configKey;
    }

    public static String appCourseListKey(Long categoryId) {
        return APP_COURSE_LIST_PREFIX + (categoryId == null ? "all" : categoryId);
    }

    public static String appPlanDetailStaticKey(Long planId) {
        return APP_PLAN_DETAIL_STATIC_PREFIX + planId;
    }

    public static String appPlanDetailLockKey(Long planId) {
        return APP_PLAN_DETAIL_LOCK_PREFIX + planId;
    }

    public static String appSmsCodeKey(String scene, String phone) {
        return APP_SMS_CODE_PREFIX + scene + ":" + phone;
    }

    public static String appSmsCooldownKey(String scene, String phone) {
        return APP_SMS_COOLDOWN_PREFIX + scene + ":" + phone;
    }

    public static String appSmsFailKey(String scene, String phone) {
        return APP_SMS_FAIL_PREFIX + scene + ":" + phone;
    }

    public static String appLoginLimitAccountKey(String account) {
        return APP_LOGIN_LIMIT_ACCOUNT_PREFIX + normalizeKeyPart(account);
    }

    public static String appLoginLimitIpKey(String clientIp) {
        return APP_LOGIN_LIMIT_IP_PREFIX + normalizeKeyPart(clientIp);
    }

    public static String adminLoginLimitAccountKey(String account) {
        return ADMIN_LOGIN_LIMIT_ACCOUNT_PREFIX + normalizeKeyPart(account);
    }

    public static String adminLoginLimitIpKey(String clientIp) {
        return ADMIN_LOGIN_LIMIT_IP_PREFIX + normalizeKeyPart(clientIp);
    }

    public static String appSmsSendLimitKey(String scene, String phone) {
        return APP_SMS_SEND_LIMIT_PREFIX + normalizeKeyPart(scene) + ":" + normalizeKeyPart(phone);
    }

    public static String appCourseEnrollRateLimitKey(Long userId) {
        return APP_COURSE_ENROLL_LIMIT_PREFIX + userId;
    }

    public static String appCourseEnrollIdempotentKey(Long userId, Long scheduleId) {
        return APP_COURSE_ENROLL_IDEMPOTENT_PREFIX + userId + ":" + scheduleId;
    }

    public static String appBookingCreateRateLimitKey(Long userId) {
        return APP_BOOKING_CREATE_LIMIT_PREFIX + userId;
    }

    public static String appBookingCreateIdempotentKey(Long userId, Long scheduleId) {
        return APP_BOOKING_CREATE_IDEMPOTENT_PREFIX + userId + ":" + scheduleId;
    }

    public static String appPayCallbackBookingGuardKey(Long bookingId) {
        return APP_PAY_CALLBACK_GUARD_PREFIX + "booking:" + bookingId;
    }

    public static String appPayCallbackPayNoGuardKey(String payNo) {
        return APP_PAY_CALLBACK_GUARD_PREFIX + "payno:" + normalizeKeyPart(payNo);
    }

    public static String taskLockKey(String taskCode) {
        return TASK_LOCK_PREFIX + normalizeKeyPart(taskCode);
    }

    private static String normalizeKeyPart(String value) {
        if (value == null) {
            return "unknown";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "unknown" : normalized.replace(' ', '_');
    }
}
