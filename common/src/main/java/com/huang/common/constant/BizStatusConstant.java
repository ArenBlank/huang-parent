package com.huang.common.constant;

public final class BizStatusConstant {

    private BizStatusConstant() {
    }

    public static final class OrderStatus {
        public static final String NEW = "NEW";
        public static final String UNPAID = "UNPAID";
        public static final String PAID = "PAID";
        public static final String CLOSED = "CLOSED";
        public static final String CANCELLED = "CANCELLED";
        public static final String REFUNDED = "REFUNDED";

        private OrderStatus() {
        }
    }

    public static final class PayStatus {
        public static final String UNPAID = "UNPAID";
        public static final String PAID = "PAID";
        public static final String CLOSED = "CLOSED";
        public static final String REFUNDED = "REFUNDED";

        private PayStatus() {
        }
    }

    public static final class BookingStatus {
        public static final String WAIT_PAY = "WAIT_PAY";
        public static final String PAID = "PAID";
        public static final String COMPLETED = "COMPLETED";
        public static final String CANCELLED = "CANCELLED";

        private BookingStatus() {
        }
    }

    public static final class EnrollmentStatus {
        public static final int CANCELED = 0;
        public static final int UNPAID = 1;
        public static final int PAID = 2;
        public static final int REFUNDED = 3;

        private EnrollmentStatus() {
        }
    }

    public static final class BizType {
        public static final String COACH_BOOKING = "coach_booking";
        public static final String COURSE_ENROLLMENT = "course_enrollment";
        public static final String COURSE = "course";

        private BizType() {
        }
    }

    public static final class RefundStatus {
        public static final String REFUNDED = "REFUNDED";

        private RefundStatus() {
        }
    }
}

