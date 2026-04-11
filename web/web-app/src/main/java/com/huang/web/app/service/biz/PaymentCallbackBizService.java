package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.CourseEnrollment;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentCallbackLog;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.dto.pay.PayCallbackDTO;
import com.huang.web.app.mapper.CoachBookingMapper;
import com.huang.web.app.mapper.CourseEnrollmentMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.PaymentCallbackLogMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.service.pay.PaymentSignVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class PaymentCallbackBizService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackBizService.class);

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final CoachBookingMapper coachBookingMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;
    private final PaymentSignVerifier paymentSignVerifier;
    private final RedisGuardSupport redisGuardSupport;

    public PaymentCallbackBizService(PaymentRecordMapper paymentRecordMapper,
                                     OrderInfoMapper orderInfoMapper,
                                     CoachBookingMapper coachBookingMapper,
                                     CourseEnrollmentMapper courseEnrollmentMapper,
                                     PaymentCallbackLogMapper paymentCallbackLogMapper,
                                     PaymentSignVerifier paymentSignVerifier,
                                     RedisGuardSupport redisGuardSupport) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.coachBookingMapper = coachBookingMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
        this.paymentSignVerifier = paymentSignVerifier;
        this.redisGuardSupport = redisGuardSupport;
    }

    @Transactional(rollbackFor = Exception.class)
    public String handleCallback(PayCallbackDTO dto, String payload) {
        long start = System.currentTimeMillis();
        String result = "fail";
        String callbackGuardKey = null;
        String callbackLockToken = null;
        try {
            PaymentCallbackLog log = baseLog(dto, payload);

            boolean signValid = verifySign(dto);
            log.setSignValid(signValid ? 1 : 0);
            if (!signValid) {
                log.setProcessResult("REJECTED");
                log.setErrorMessage("INVALID_SIGN");
                paymentCallbackLogMapper.insert(log);
                result = "fail";
                return result;
            }

            PaymentRecord paymentRecord = paymentRecordMapper.selectOne(
                    new LambdaQueryWrapper<PaymentRecord>()
                            .eq(PaymentRecord::getPayNo, dto.getPayNo())
                            .last("LIMIT 1")
            );
            if (paymentRecord == null) {
                log.setProcessResult("REJECTED");
                log.setErrorMessage("PAY_NO_NOT_FOUND");
                paymentCallbackLogMapper.insert(log);
                result = "fail";
                return result;
            }

            log.setOrderId(paymentRecord.getOrderId());
            callbackGuardKey = RedisConstant.appPayCallbackPayNoGuardKey(paymentRecord.getPayNo());
            callbackLockToken = redisGuardSupport.tryAcquireLock(callbackGuardKey, RedisConstant.PAY_CALLBACK_GUARD_TTL_SEC);
            if (callbackLockToken == null) {
                log.setProcessResult("IN_FLIGHT");
                paymentCallbackLogMapper.insert(log);
                result = "success";
                return result;
            }
            if (!"SUCCESS".equalsIgnoreCase(dto.getStatus())) {
                log.setProcessResult("IGNORED");
                log.setErrorMessage("STATUS_NOT_SUCCESS");
                paymentCallbackLogMapper.insert(log);
                result = "success";
                return result;
            }

            if (dto.getAmount() != null && paymentRecord.getPayAmount() != null) {
                if (dto.getAmount().compareTo(paymentRecord.getPayAmount()) != 0) {
                    log.setProcessResult("REJECTED");
                    log.setErrorMessage("AMOUNT_MISMATCH");
                    paymentCallbackLogMapper.insert(log);
                    result = "fail";
                    return result;
                }
            }

            if (BizStatusConstant.PayStatus.PAID.equals(paymentRecord.getPayStatus())) {
                log.setProcessResult("IDEMPOTENT");
                paymentCallbackLogMapper.insert(log);
                result = "success";
                return result;
            }

            String idempotencyKey = "CALLBACK_ASYNC_" + dto.getTradeNo();
            if (idempotencyKey.equals(paymentRecord.getCallbackIdempotencyKey())) {
                log.setProcessResult("IDEMPOTENT");
                paymentCallbackLogMapper.insert(log);
                result = "success";
                return result;
            }

            paymentRecord.setPayStatus(BizStatusConstant.PayStatus.PAID);
            paymentRecord.setPayTime(LocalDateTime.now());
            paymentRecord.setCallbackIdempotencyKey(idempotencyKey);
            paymentRecordMapper.updateById(paymentRecord);

            OrderInfo orderInfo = orderInfoMapper.selectById(paymentRecord.getOrderId());
            if (orderInfo != null) {
                orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
                orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
                orderInfoMapper.updateById(orderInfo);
                updateBizStatus(orderInfo);
            }

            log.setProcessResult("PROCESSED");
            paymentCallbackLogMapper.insert(log);
            result = "success";
            return result;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            log.info("PAY_CALLBACK payNo={} tradeNo={} status={} result={} costMs={}",
                    dto == null ? null : dto.getPayNo(),
                    dto == null ? null : dto.getTradeNo(),
                    dto == null ? null : dto.getStatus(),
                    result,
                    costMs);
            redisGuardSupport.releaseLock(callbackGuardKey, callbackLockToken);
        }
    }

    public PayCallbackDTO buildMockCallback(String payNo, String tradeNo) {
        PaymentRecord paymentRecord = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPayNo, payNo)
                        .last("LIMIT 1")
        );
        if (paymentRecord == null) {
            return null;
        }
        PayCallbackDTO dto = new PayCallbackDTO();
        dto.setPayNo(payNo);
        dto.setTradeNo((tradeNo == null || tradeNo.isBlank()) ? ("MOCKTRADE" + System.currentTimeMillis()) : tradeNo);
        dto.setStatus("SUCCESS");
        dto.setAmount(paymentRecord.getPayAmount() == null ? BigDecimal.ZERO : paymentRecord.getPayAmount());
        dto.setSign(paymentSignVerifier.generateSign(dto));
        return dto;
    }

    private void updateBizStatus(OrderInfo orderInfo) {
        if (BizStatusConstant.BizType.COACH_BOOKING.equals(orderInfo.getBizType())) {
            CoachBooking booking = coachBookingMapper.selectById(orderInfo.getBizId());
            if (booking != null) {
                booking.setPayStatus(BizStatusConstant.PayStatus.PAID);
                booking.setBookingStatus(BizStatusConstant.BookingStatus.PAID);
                coachBookingMapper.updateById(booking);
            }
            return;
        }

        String bizType = orderInfo.getBizType() == null ? "" : orderInfo.getBizType().toLowerCase(Locale.ROOT);
        if (BizStatusConstant.BizType.COURSE_ENROLLMENT.equals(bizType)
                || BizStatusConstant.BizType.COURSE.equals(bizType)) {
            CourseEnrollment enrollment = courseEnrollmentMapper.selectById(orderInfo.getBizId());
            if (enrollment != null) {
                enrollment.setStatus(BizStatusConstant.EnrollmentStatus.PAID);
                courseEnrollmentMapper.updateById(enrollment);
            }
        }
    }

    private boolean verifySign(PayCallbackDTO dto) { return paymentSignVerifier.verify(dto); }

    private PaymentCallbackLog baseLog(PayCallbackDTO dto, String payload) {
        PaymentCallbackLog log = new PaymentCallbackLog();
        log.setPayNo(dto.getPayNo());
        log.setChannelTradeNo(dto.getTradeNo());
        log.setCallbackStatus(dto.getStatus());
        log.setCallbackPayload(payload);
        log.setNotifiedAt(LocalDateTime.now());
        return log;
    }
}
