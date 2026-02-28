package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.*;
import com.huang.web.app.dto.booking.BookingReviewDTO;
import com.huang.web.app.dto.booking.CreateBookingDTO;
import com.huang.web.app.mapper.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class BookingBizService {

    private final CoachScheduleMapper coachScheduleMapper;
    private final CoachBookingMapper coachBookingMapper;
    private final CoachReviewMapper coachReviewMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public BookingBizService(CoachScheduleMapper coachScheduleMapper,
                             CoachBookingMapper coachBookingMapper,
                             CoachReviewMapper coachReviewMapper,
                             OrderInfoMapper orderInfoMapper,
                             OrderItemMapper orderItemMapper,
                             PaymentRecordMapper paymentRecordMapper,
                             RedisTemplate<String, Object> redisTemplate) {
        this.coachScheduleMapper = coachScheduleMapper;
        this.coachBookingMapper = coachBookingMapper;
        this.coachReviewMapper = coachReviewMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.redisTemplate = redisTemplate;
    }

    public List<CoachSchedule> listSchedule(Long coachId, LocalDate date) {
        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getStatus, 1)
                .orderByAsc(CoachSchedule::getScheduleDate, CoachSchedule::getStartTime);
        if (coachId != null) {
            wrapper.eq(CoachSchedule::getCoachId, coachId);
        }
        if (date != null) {
            wrapper.eq(CoachSchedule::getScheduleDate, date);
        }
        return coachScheduleMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createBooking(Long userId, CreateBookingDTO dto) {
        CoachSchedule schedule = coachScheduleMapper.selectById(dto.getScheduleId());
        if (schedule == null || schedule.getStatus() == null || schedule.getStatus() != 1) {
            return null;
        }

        // 工程亮点：用单条原子更新保护容量，避免并发下 booked_count 超过 capacity（超卖）。
        int updated = coachScheduleMapper.update(
                null,
                new LambdaUpdateWrapper<CoachSchedule>()
                        .eq(CoachSchedule::getId, schedule.getId())
                        .eq(CoachSchedule::getStatus, 1)
                        .apply("booked_count < capacity")
                        .setSql("booked_count = booked_count + 1")
        );
        if (updated == 0) {
            return null;
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(genNo("ORD"));
        orderInfo.setUserId(userId);
        orderInfo.setTotalAmount(schedule.getPrice() == null ? BigDecimal.ZERO : schedule.getPrice());
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.NEW);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        orderInfo.setBizType(BizStatusConstant.BizType.COACH_BOOKING);
        orderInfoMapper.insert(orderInfo);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderInfo.getId());
        orderItem.setItemType("coach_booking");
        orderItem.setItemId(schedule.getId());
        orderItem.setItemName("Coach booking #" + schedule.getId());
        orderItem.setPrice(orderInfo.getTotalAmount());
        orderItem.setQuantity(1);
        orderItem.setAmount(orderInfo.getTotalAmount());
        orderItemMapper.insert(orderItem);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(orderInfo.getId());
        paymentRecord.setPayNo(genNo("PAY"));
        paymentRecord.setPayChannel("wechat");
        paymentRecord.setPayAmount(orderInfo.getTotalAmount());
        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        paymentRecordMapper.insert(paymentRecord);

        CoachBooking booking = new CoachBooking();
        booking.setUserId(userId);
        booking.setCoachId(schedule.getCoachId());
        booking.setScheduleId(schedule.getId());
        booking.setOrderId(orderInfo.getId());
        booking.setBookingStatus(BizStatusConstant.BookingStatus.WAIT_PAY);
        booking.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        coachBookingMapper.insert(booking);

        orderInfo.setBizId(booking.getId());
        orderInfoMapper.updateById(orderInfo);

        Map<String, Object> result = new HashMap<>();
        result.put("bookingId", booking.getId());
        result.put("orderId", orderInfo.getId());
        result.put("orderNo", orderInfo.getOrderNo());
        result.put("payNo", paymentRecord.getPayNo());
        result.put("amount", orderInfo.getTotalAmount());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaySuccess(Long bookingId, Long userId) {
        String callbackKey = "pay:callback:booking:" + bookingId;
        try {
            // 工程亮点：Redis 短期幂等键，先挡住大部分重复回调，降低 DB 冲突与重复写压力。
            Boolean first = redisTemplate.opsForValue().setIfAbsent(callbackKey, "1", 10, TimeUnit.MINUTES);
            if (Boolean.FALSE.equals(first)) {
                return true;
            }
        } catch (Exception ignore) {
            // Redis unavailable: fallback to DB idempotency key only.
        }

        CoachBooking booking = coachBookingMapper.selectById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            return false;
        }
        PaymentRecord paymentRecord = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, booking.getOrderId())
                        .last("LIMIT 1")
        );
        if (paymentRecord == null) {
            return false;
        }

        String idempotencyKey = "CALLBACK_" + paymentRecord.getPayNo();
        // 工程亮点：DB 幂等键二次兜底；即使 Redis 失效，也可保证回调“至多一次”生效。
        if (idempotencyKey.equals(paymentRecord.getCallbackIdempotencyKey())
                || BizStatusConstant.PayStatus.PAID.equals(paymentRecord.getPayStatus())) {
            return true;
        }

        booking.setPayStatus(BizStatusConstant.PayStatus.PAID);
        booking.setBookingStatus(BizStatusConstant.BookingStatus.PAID);
        coachBookingMapper.updateById(booking);

        OrderInfo orderInfo = orderInfoMapper.selectById(booking.getOrderId());
        if (orderInfo != null) {
            orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
            orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
            orderInfoMapper.updateById(orderInfo);
        }

        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.PAID);
        paymentRecord.setPayTime(LocalDateTime.now());
        paymentRecord.setCallbackIdempotencyKey(idempotencyKey);
        paymentRecordMapper.updateById(paymentRecord);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeBooking(Long bookingId, Long userId) {
        CoachBooking booking = coachBookingMapper.selectById(bookingId);
        if (booking == null || !booking.getUserId().equals(userId)) {
            return false;
        }
        if (!BizStatusConstant.PayStatus.PAID.equals(booking.getPayStatus())) {
            return false;
        }
        booking.setBookingStatus(BizStatusConstant.BookingStatus.COMPLETED);
        booking.setFinishTime(LocalDateTime.now());
        return coachBookingMapper.updateById(booking) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean review(Long userId, BookingReviewDTO dto) {
        CoachBooking booking = coachBookingMapper.selectById(dto.getBookingId());
        if (booking == null || !booking.getUserId().equals(userId)) {
            return false;
        }
        if (!BizStatusConstant.BookingStatus.COMPLETED.equals(booking.getBookingStatus())) {
            return false;
        }
        // 工程亮点：服务完成后才能评价，防止“未履约先评价”污染评分体系。
        CoachReview existed = coachReviewMapper.selectOne(
                new LambdaQueryWrapper<CoachReview>()
                        .eq(CoachReview::getBookingId, dto.getBookingId())
                        .last("LIMIT 1")
        );
        if (existed != null) {
            return false;
        }
        CoachReview review = new CoachReview();
        review.setBookingId(dto.getBookingId());
        review.setUserId(booking.getUserId());
        review.setCoachId(booking.getCoachId());
        review.setScore(dto.getScore());
        review.setContent(dto.getContent());
        return coachReviewMapper.insert(review) > 0;
    }

    public List<CoachBooking> myBookings(Long userId) {
        return coachBookingMapper.selectList(
                new LambdaQueryWrapper<CoachBooking>()
                        .eq(CoachBooking::getUserId, userId)
                        .orderByDesc(CoachBooking::getId)
        );
    }

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().toString().replace("-", "").replace(":", "").replace("T", "").replace(".", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
