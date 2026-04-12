package com.huang.common.constant;

public final class TaskRunConstant {

    private TaskRunConstant() {
    }

    public static final String TASK_PAYMENT_COMPENSATE = "PAYMENT_COMPENSATE";
    public static final String TASK_BOOKING_TIMEOUT_CLOSE = "BOOKING_TIMEOUT_CLOSE";

    public static final String TASK_PAYMENT_COMPENSATE_NAME = "Payment compensation";
    public static final String TASK_BOOKING_TIMEOUT_CLOSE_NAME = "Booking timeout close";

    public static final String TRIGGER_SCHEDULED = "SCHEDULED";
    public static final String TRIGGER_MANUAL = "MANUAL";

    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    public static long lockTtlSec(String taskCode) {
        if (TASK_PAYMENT_COMPENSATE.equalsIgnoreCase(taskCode)) {
            return 300L;
        }
        if (TASK_BOOKING_TIMEOUT_CLOSE.equalsIgnoreCase(taskCode)) {
            return 180L;
        }
        return 300L;
    }
}
