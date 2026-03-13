package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.CoachSchedule;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.OrderItem;
import com.huang.model.entity.PaymentRecord;
import com.huang.model.entity.TrainingRecord;
import com.huang.model.entity.User;
import com.huang.web.admin.mapper.CoachBookingMapper;
import com.huang.web.admin.mapper.CoachScheduleMapper;
import com.huang.web.admin.mapper.OrderInfoMapper;
import com.huang.web.admin.mapper.OrderItemMapper;
import com.huang.web.admin.mapper.PaymentRecordMapper;
import com.huang.web.admin.mapper.TrainingRecordMapper;
import com.huang.web.admin.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    public AdminOpsBizService(CoachBookingMapper coachBookingMapper,
                              OrderInfoMapper orderInfoMapper,
                              OrderItemMapper orderItemMapper,
                              TrainingRecordMapper trainingRecordMapper,
                              UserMapper userMapper,
                              CoachScheduleMapper coachScheduleMapper,
                              PaymentRecordMapper paymentRecordMapper) {
        this.coachBookingMapper = coachBookingMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.trainingRecordMapper = trainingRecordMapper;
        this.userMapper = userMapper;
        this.coachScheduleMapper = coachScheduleMapper;
        this.paymentRecordMapper = paymentRecordMapper;
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
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        return result;
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
