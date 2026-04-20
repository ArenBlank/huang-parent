package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.CoachReview;
import com.huang.model.entity.CoachSchedule;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.OrderItem;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.dto.booking.BookingReviewDTO;
import com.huang.web.app.dto.booking.CreateBookingDTO;
import com.huang.web.app.mapper.CoachBookingMapper;
import com.huang.web.app.mapper.CoachReviewMapper;
import com.huang.web.app.mapper.CoachScheduleMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.OrderItemMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.vo.booking.BookingHistoryVO;
import com.huang.web.app.vo.booking.BookingScheduleSummaryVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BookingBizService {

    private static final Logger log = LoggerFactory.getLogger(BookingBizService.class);

    private final CoachScheduleMapper coachScheduleMapper;
    private final CoachBookingMapper coachBookingMapper;
    private final CoachReviewMapper coachReviewMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RedisGuardSupport redisGuardSupport;

    public BookingBizService(CoachScheduleMapper coachScheduleMapper,
                             CoachBookingMapper coachBookingMapper,
                             CoachReviewMapper coachReviewMapper,
                             OrderInfoMapper orderInfoMapper,
                             OrderItemMapper orderItemMapper,
                             PaymentRecordMapper paymentRecordMapper,
                             RedisGuardSupport redisGuardSupport) {
        this.coachScheduleMapper = coachScheduleMapper;
        this.coachBookingMapper = coachBookingMapper;
        this.coachReviewMapper = coachReviewMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.redisGuardSupport = redisGuardSupport;
    }

    public List<CoachSchedule> listSchedule(Long userId, Long coachId, LocalDate date) {
        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getStatus, 1)
                .apply("booked_count < capacity")
                .orderByAsc(CoachSchedule::getScheduleDate, CoachSchedule::getStartTime);
        if (coachId != null) {
            wrapper.eq(CoachSchedule::getCoachId, coachId);
        }
        if (date != null) {
            wrapper.eq(CoachSchedule::getScheduleDate, date);
        }
        if (userId != null) {
            wrapper.apply(
                    "NOT EXISTS (SELECT 1 FROM coach_booking cb WHERE cb.schedule_id = coach_schedule.id AND cb.user_id = {0})",
                    userId
            );
        }
        return coachScheduleMapper.selectList(wrapper);
    }

    public BookingScheduleSummaryVO scheduleSummary(Long userId, Long coachId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        LambdaQueryWrapper<CoachSchedule> baseWrapper = new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getStatus, 1);
        if (coachId != null) {
            baseWrapper.eq(CoachSchedule::getCoachId, coachId);
        }

        List<CoachSchedule> schedules = coachScheduleMapper.selectList(
                baseWrapper.orderByAsc(CoachSchedule::getScheduleDate, CoachSchedule::getStartTime)
        );

        long futureSchedules = schedules.stream()
                .filter(item -> isFutureSchedule(item, today, now))
                .count();

        long availableSchedules = schedules.stream()
                .filter(item -> isFutureSchedule(item, today, now))
                .filter(item -> hasCapacity(item))
                .filter(item -> !hasBooked(userId, item.getId()))
                .count();

        BookingScheduleSummaryVO vo = new BookingScheduleSummaryVO();
        vo.setCoachId(coachId);
        vo.setTotalSchedules((long) schedules.size());
        vo.setFutureSchedules(futureSchedules);
        vo.setAvailableSchedules(availableSchedules);
        vo.setLastScheduleDate(
                schedules.stream()
                        .map(CoachSchedule::getScheduleDate)
                        .filter(Objects::nonNull)
                        .max(LocalDate::compareTo)
                        .orElse(null)
        );
        vo.setNextScheduleDate(
                schedules.stream()
                        .filter(item -> isFutureSchedule(item, today, now))
                        .map(CoachSchedule::getScheduleDate)
                        .filter(Objects::nonNull)
                        .min(LocalDate::compareTo)
                        .orElse(null)
        );
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createBooking(Long userId, CreateBookingDTO dto) {
        long start = System.currentTimeMillis();
        Long bookingId = null;
        boolean success = false;
        String reason = null;
        try {
            CoachSchedule schedule = coachScheduleMapper.selectById(dto.getScheduleId());
            if (schedule == null || schedule.getStatus() == null || schedule.getStatus() != 1) {
                reason = "schedule_invalid";
                return null;
            }

            Long duplicated = coachBookingMapper.countAnyByUserAndSchedule(userId, schedule.getId());
            if (duplicated != null && duplicated > 0) {
                reason = "schedule_already_booked";
                return null;
            }

            int updated = coachScheduleMapper.reserveSlot(schedule.getId());
            if (updated == 0) {
                reason = "schedule_full";
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

            bookingId = booking.getId();
            orderInfo.setBizId(bookingId);
            orderInfoMapper.updateById(orderInfo);

            Map<String, Object> result = new HashMap<>();
            result.put("bookingId", bookingId);
            result.put("orderId", orderInfo.getId());
            result.put("orderNo", orderInfo.getOrderNo());
            result.put("payNo", paymentRecord.getPayNo());
            result.put("amount", orderInfo.getTotalAmount());
            success = true;
            return result;
        } catch (DataIntegrityViolationException e) {
            markCurrentTransactionRollbackOnly();
            reason = "duplicate_booking_fallback";
            log.info("BOOKING_CREATE duplicate fallback triggered, userId={} scheduleId={} message={}",
                    userId,
                    dto == null ? null : dto.getScheduleId(),
                    e.getMessage());
            return null;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            log.info("BOOKING_CREATE userId={} scheduleId={} success={} bookingId={} reason={} costMs={}",
                    userId,
                    dto == null ? null : dto.getScheduleId(),
                    success,
                    bookingId,
                    reason,
                    costMs);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaySuccess(Long bookingId, Long userId) {
        long start = System.currentTimeMillis();
        boolean success = false;
        String callbackKey = RedisConstant.appPayCallbackBookingGuardKey(bookingId);
        try {
            try {
                if (!redisGuardSupport.tryAcquireIdempotent(callbackKey, RedisConstant.PAY_CALLBACK_GUARD_TTL_SEC)) {
                    success = true;
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
            if (idempotencyKey.equals(paymentRecord.getCallbackIdempotencyKey())
                    || BizStatusConstant.PayStatus.PAID.equals(paymentRecord.getPayStatus())) {
                success = true;
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
            success = true;
            return true;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            log.info("BOOKING_PAY_SUCCESS userId={} bookingId={} success={} costMs={}",
                    userId, bookingId, success, costMs);
        }
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

    public List<BookingHistoryVO> myBookings(Long userId) {
        List<CoachBooking> bookings = coachBookingMapper.selectList(
                new LambdaQueryWrapper<CoachBooking>()
                        .eq(CoachBooking::getUserId, userId)
                        .orderByDesc(CoachBooking::getId)
        );
        if (bookings == null || bookings.isEmpty()) {
            return List.of();
        }

        Set<Long> scheduleIds = bookings.stream()
                .map(CoachBooking::getScheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> orderIds = bookings.stream()
                .map(CoachBooking::getOrderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, CoachSchedule> scheduleMap = scheduleIds.isEmpty()
                ? Map.of()
                : coachScheduleMapper.selectList(new LambdaQueryWrapper<CoachSchedule>().in(CoachSchedule::getId, scheduleIds))
                .stream()
                .collect(Collectors.toMap(CoachSchedule::getId, item -> item));

        Map<Long, OrderInfo> orderMap = orderIds.isEmpty()
                ? Map.of()
                : orderInfoMapper.selectList(new LambdaQueryWrapper<OrderInfo>().in(OrderInfo::getId, orderIds))
                .stream()
                .collect(Collectors.toMap(OrderInfo::getId, item -> item));

        return bookings.stream()
                .map(item -> toBookingHistoryVO(item, scheduleMap.get(item.getScheduleId()), orderMap.get(item.getOrderId())))
                .toList();
    }

    private BookingHistoryVO toBookingHistoryVO(CoachBooking booking, CoachSchedule schedule, OrderInfo orderInfo) {
        BookingHistoryVO vo = new BookingHistoryVO();
        vo.setId(booking.getId());
        vo.setOrderId(booking.getOrderId());
        vo.setOrderNo(orderInfo == null ? null : orderInfo.getOrderNo());
        vo.setAmount(orderInfo == null ? null : orderInfo.getTotalAmount());
        vo.setCoachId(booking.getCoachId());
        vo.setScheduleId(booking.getScheduleId());
        if (schedule != null) {
            vo.setScheduleDate(schedule.getScheduleDate());
            vo.setStartTime(schedule.getStartTime());
            vo.setEndTime(schedule.getEndTime());
        }
        vo.setBookingStatus(booking.getBookingStatus());
        vo.setPayStatus(booking.getPayStatus());
        vo.setCreateTime(booking.getCreateTime());
        vo.setFinishTime(booking.getFinishTime());
        return vo;
    }

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().toString().replace("-", "").replace(":", "").replace("T", "").replace(".", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private boolean isFutureSchedule(CoachSchedule schedule, LocalDate today, LocalTime now) {
        if (schedule == null || schedule.getScheduleDate() == null) {
            return false;
        }
        if (schedule.getScheduleDate().isAfter(today)) {
            return true;
        }
        if (!schedule.getScheduleDate().isEqual(today)) {
            return false;
        }
        LocalTime endTime = schedule.getEndTime() == null ? LocalTime.MIN : schedule.getEndTime();
        return endTime.isAfter(now.minusMinutes(1));
    }

    private boolean hasCapacity(CoachSchedule schedule) {
        int capacity = schedule.getCapacity() == null ? 0 : schedule.getCapacity();
        int bookedCount = schedule.getBookedCount() == null ? 0 : schedule.getBookedCount();
        return bookedCount < capacity;
    }

    private boolean hasBooked(Long userId, Long scheduleId) {
        if (userId == null || scheduleId == null) {
            return false;
        }
        Long count = coachBookingMapper.countAnyByUserAndSchedule(userId, scheduleId);
        return count != null && count > 0;
    }

    private void markCurrentTransactionRollbackOnly() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }
}
