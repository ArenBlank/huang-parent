package com.huang.web.admin.constant;

public final class AdminErrorCode {

    private AdminErrorCode() {
    }

    public static final int COACH_APPLY_AUDIT_FAILED = 7301;

    public static final int COURSE_NOT_FOUND = 7401;
    public static final int COURSE_STATUS_INVALID = 7402;
    public static final int COURSE_DELETE_FORBIDDEN = 7403;
    public static final int COURSE_SCHEDULE_NOT_FOUND = 7411;
    public static final int COURSE_SCHEDULE_STATUS_INVALID = 7412;

    public static final int VIDEO_ASSET_NOT_FOUND = 7501;
    public static final int VIDEO_BIND_FAILED = 7502;

    public static final int TRAINING_PLAN_NOT_FOUND = 7551;
    public static final int TRAINING_PLAN_ITEM_NOT_FOUND = 7552;
    public static final int TRAINING_PLAN_DELETE_FAILED = 7553;

    public static final int BOOKING_COMPLETE_FAILED = 7601;
    public static final int ORDER_NOT_FOUND = 7602;
    public static final int COACH_SCHEDULE_NOT_FOUND = 7611;
    public static final int COACH_SCHEDULE_STATUS_INVALID = 7612;
    public static final int COACH_SCHEDULE_CONFLICT = 7613;
    public static final int COACH_SCHEDULE_DELETE_FORBIDDEN = 7614;
    public static final int COACH_SCHEDULE_UPDATE_FORBIDDEN = 7615;
    public static final int COACH_SCHEDULE_COACH_INVALID = 7616;

    public static final int BANNER_NOT_FOUND = 7701;
    public static final int BANNER_STATUS_INVALID = 7702;

    public static final int NOTICE_NOT_FOUND = 7801;
    public static final int NOTICE_STATUS_INVALID = 7802;

    public static final int SYSTEM_CONFIG_NOT_FOUND = 7901;
    public static final int SYSTEM_CONFIG_KEY_DUPLICATE = 7902;
    public static final int SYSTEM_CONFIG_UPDATE_FAILED = 7903;
}
