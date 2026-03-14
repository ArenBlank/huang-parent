package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.CoachSchedule;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.OrderItem;
import com.huang.model.entity.PaymentRecord;
import com.huang.model.entity.RefundRecord;
import com.huang.model.entity.TrainingRecord;
import com.huang.model.entity.User;
import com.huang.web.admin.mapper.CoachBookingMapper;
import com.huang.web.admin.mapper.CoachScheduleMapper;
import com.huang.web.admin.mapper.OrderInfoMapper;
import com.huang.web.admin.mapper.OrderItemMapper;
import com.huang.web.admin.mapper.PaymentRecordMapper;
import com.huang.web.admin.mapper.RefundRecordMapper;
import com.huang.web.admin.mapper.TrainingRecordMapper;
import com.huang.web.admin.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminOpsBizService {

    private final CoachBookingMapper coachBookingMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final TrainingRecordMapper trainingRecordMapper;
    private final UserMapper userMapper;
    private final CoachScheduleMapper coachScheduleMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderItemMapper orderItemMapper;
    private final RefundRecordMapper refundRecordMapper;

    public AdminOpsBizService(CoachBookingMapper coachBookingMapper,
                              OrderInfoMapper orderInfoMapper,
                              OrderItemMapper orderItemMapper,
                              TrainingRecordMapper trainingRecordMapper,
                              UserMapper userMapper,
                              CoachScheduleMapper coachScheduleMapper,
                              PaymentRecordMapper paymentRecordMapper,
                              RefundRecordMapper refundRecordMapper) {
        this.coachBookingMapper = coachBookingMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.trainingRecordMapper = trainingRecordMapper;
        this.userMapper = userMapper;
        this.coachScheduleMapper = coachScheduleMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundRecordMapper = refundRecordMapper;
    }

    public List<CoachBooking> bookingList(String status) {
        LambdaQueryWrapper<CoachBooking> wrapper = new LambdaQueryWrapper<CoachBooking>()
                .orderByDesc(CoachBooking::getId);
        if (status != null && !status.isBlank()) {
            wrapper.eq(CoachBooking::getBookingStatus, status);
        }
        return coachBookingMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markBookingCompleted(Long bookingId) {
        CoachBooking booking = coachBookingMapper.selectById(bookingId);
        if (booking == null || !BizStatusConstant.PayStatus.PAID.equals(booking.getPayStatus())) {
            return false;
        }
        booking.setBookingStatus(BizStatusConstant.BookingStatus.COMPLETED);
        booking.setFinishTime(LocalDateTime.now());
        return coachBookingMapper.updateById(booking) > 0;
    }

    public List<OrderInfo> orderList(String payStatus) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .orderByDesc(OrderInfo::getId);
        if (payStatus != null && !payStatus.isBlank()) {
            wrapper.eq(OrderInfo::getPayStatus, payStatus);
        }
        return orderInfoMapper.selectList(wrapper);
    }

    public Map<String, Object> orderDetail(Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null) {
            return null;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId)
        );
        PaymentRecord payment = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderId)
                        .orderByDesc(PaymentRecord::getId)
                        .last("LIMIT 1")
        );
        RefundRecord refund = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>()
                        .eq(RefundRecord::getOrderId, orderId)
                        .orderByDesc(RefundRecord::getId)
                        .last("LIMIT 1")
        );
        BigDecimal paidAmount = payment != null && payment.getPayAmount() != null
                ? payment.getPayAmount()
                : BigDecimal.ZERO;
        BigDecimal refundAmount = refund != null && refund.getRefundAmount() != null
                ? refund.getRefundAmount()
                : BigDecimal.ZERO;
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("payment", payment);
        result.put("refund", refund);
        result.put("paidAmount", paidAmount);
        result.put("refundAmount", refundAmount);
        result.put("netPaid", paidAmount.subtract(refundAmount));
        result.put("payStatus", payment != null ? payment.getPayStatus() : order.getPayStatus());
        result.put("payChannel", payment != null ? payment.getPayChannel() : null);
        result.put("payTime", payment != null ? payment.getPayTime() : null);
        result.put("refundStatus", refund != null ? refund.getRefundStatus() : null);
        result.put("refundTime", refund != null ? refund.getRefundTime() : null);
        result.put("refundReason", refund != null ? refund.getReason() : null);
        Map<String, Object> financeSummary = new HashMap<>();
        financeSummary.put("paidAmount", paidAmount);
        financeSummary.put("refundAmount", refundAmount);
        financeSummary.put("netPaid", paidAmount.subtract(refundAmount));
        financeSummary.put("payStatus", payment != null ? payment.getPayStatus() : order.getPayStatus());
        financeSummary.put("refundStatus", refund != null ? refund.getRefundStatus() : null);
        financeSummary.put("statusText", buildFinanceStatusText(payment, refund, order));
        financeSummary.put("statusHint", buildFinanceStatusHint(payment, refund, order));
        financeSummary.put("statusExplain", buildFinanceStatusExplain(payment, refund, order));
        result.put("financeSummary", financeSummary);
        return result;
    }

    private String buildFinanceStatusText(PaymentRecord payment, RefundRecord refund, OrderInfo order) {
        String refundStatus = refund == null ? null : refund.getRefundStatus();
        if ("REFUNDED".equalsIgnoreCase(refundStatus)) {
            return "refunded";
        }
        String payStatus = payment == null ? null : payment.getPayStatus();
        if ("PAID".equalsIgnoreCase(payStatus)) {
            return "paid";
        }
        if ("UNPAID".equalsIgnoreCase(payStatus)) {
            return "unpaid";
        }
        String orderStatus = order == null ? null : order.getOrderStatus();
        if ("CLOSED".equalsIgnoreCase(orderStatus)) {
            return "closed";
        }
        return "unknown";
    }

    private String buildFinanceStatusHint(PaymentRecord payment, RefundRecord refund, OrderInfo order) {
        String refundStatus = refund == null ? null : refund.getRefundStatus();
        if ("REFUNDED".equalsIgnoreCase(refundStatus)) {
            return "order_refunded";
        }
        String payStatus = payment == null ? null : payment.getPayStatus();
        if ("PAID".equalsIgnoreCase(payStatus)) {
            return "order_paid";
        }
        if ("UNPAID".equalsIgnoreCase(payStatus)) {
            String orderStatus = order == null ? null : order.getOrderStatus();
            if ("CLOSED".equalsIgnoreCase(orderStatus)) {
                return "order_closed";
            }
            return "order_unpaid";
        }
        return "order_unknown";
    }

    private String buildFinanceStatusExplain(PaymentRecord payment, RefundRecord refund, OrderInfo order) {
        String refundStatus = refund == null ? null : refund.getRefundStatus();
        if ("REFUNDED".equalsIgnoreCase(refundStatus)) {
            return "已退款";
        }
        String payStatus = payment == null ? null : payment.getPayStatus();
        if ("PAID".equalsIgnoreCase(payStatus)) {
            return "已支付";
        }
        String orderStatus = order == null ? null : order.getOrderStatus();
        if ("CLOSED".equalsIgnoreCase(orderStatus)) {
            return "订单已关闭";
        }
        if ("UNPAID".equalsIgnoreCase(payStatus)) {
            return "待支付";
        }
        return "状态未知";
    }

    public Map<String, Object> dashboardSummary() {
        long userCount = userMapper.selectCount(new LambdaQueryWrapper<User>());
        long bookingCount = coachBookingMapper.selectCount(new LambdaQueryWrapper<CoachBooking>());
        long paidOrderCount = orderInfoMapper.selectCount(
                new LambdaQueryWrapper<OrderInfo>().eq(OrderInfo::getPayStatus, BizStatusConstant.PayStatus.PAID));
        long recordCount = trainingRecordMapper.selectCount(new LambdaQueryWrapper<TrainingRecord>());

        Map<String, Object> summary = new HashMap<>();
        summary.put("userCount", userCount);
        summary.put("bookingCount", bookingCount);
        summary.put("paidOrderCount", paidOrderCount);
        summary.put("trainingRecordCount", recordCount);
        return summary;
    }

    @Transactional(rollbackFor = Exception.class)
    public int closeTimeoutUnpaidBookings(int timeoutMinutes) {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(timeoutMinutes);
        List<CoachBooking> timeoutBookings = coachBookingMapper.selectList(
                new LambdaQueryWrapper<CoachBooking>()
                        .eq(CoachBooking::getPayStatus, BizStatusConstant.PayStatus.UNPAID)
                        .eq(CoachBooking::getBookingStatus, BizStatusConstant.BookingStatus.WAIT_PAY)
                        .lt(CoachBooking::getCreateTime, java.sql.Timestamp.valueOf(threshold))
        );

        int closed = 0;
        for (CoachBooking booking : timeoutBookings) {
            booking.setBookingStatus(BizStatusConstant.BookingStatus.CANCELLED);
            if (coachBookingMapper.updateById(booking) > 0) {
                closed++;
                orderInfoMapper.update(
                        null,
                        new LambdaUpdateWrapper<OrderInfo>()
                                .eq(OrderInfo::getId, booking.getOrderId())
                                .set(OrderInfo::getOrderStatus, BizStatusConstant.OrderStatus.CANCELLED)
                );
                paymentRecordMapper.update(
                        null,
                        new LambdaUpdateWrapper<PaymentRecord>()
                                .eq(PaymentRecord::getOrderId, booking.getOrderId())
                                .set(PaymentRecord::getPayStatus, BizStatusConstant.PayStatus.CLOSED)
                );
                coachScheduleMapper.update(
                        null,
                        new LambdaUpdateWrapper<CoachSchedule>()
                                .eq(CoachSchedule::getId, booking.getScheduleId())
                                .gt(CoachSchedule::getBookedCount, 0)
                                .setSql("booked_count = booked_count - 1")
                );
            }
        }
        return closed;
    }
}
