package com.huang.common.constant;

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

    public static final long APP_BANNER_TTL_SEC = 15 * 60;
    public static final long APP_NOTICE_TTL_SEC = 10 * 60;
    public static final long APP_SYS_CONFIG_TTL_SEC = 60 * 60;
    public static final long APP_COURSE_LIST_TTL_SEC = 10 * 60;
    public static final long APP_PLAN_LIST_TTL_SEC = 10 * 60;
    public static final long APP_PLAN_DETAIL_TTL_SEC = 15 * 60;

    public static final long JITTER_SHORT_SEC = 180;
    public static final long JITTER_LONG_SEC = 300;

    public static final String APP_BANNER_ACTIVE_KEY = "app:banner:active";
    public static final String APP_NOTICE_PUBLISHED_PREFIX = "app:notice:published:";
    public static final String APP_SYS_CONFIG_PREFIX = "app:sys-config:";
    public static final String APP_COURSE_LIST_PREFIX = "app:course:list:";
    public static final String APP_PLAN_LIST_ACTIVE_KEY = "app:plan:list:active";
    public static final String APP_PLAN_DETAIL_STATIC_PREFIX = "app:plan:detail:static:";
    public static final String APP_PLAN_DETAIL_LOCK_PREFIX = "app:plan:detail:lock:";
    public static final String APP_SMS_CODE_PREFIX = "app:sms:code:";
    public static final String APP_SMS_COOLDOWN_PREFIX = "app:sms:cooldown:";
    public static final String APP_SMS_FAIL_PREFIX = "app:sms:fail:";

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
}
