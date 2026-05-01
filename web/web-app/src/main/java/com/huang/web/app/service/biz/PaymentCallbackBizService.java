package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.LockAcquireResult;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentCallbackLog;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.dto.pay.PayCallbackDTO;
import com.huang.web.app.mapper.CoachBookingMapper;
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
    private final PaymentCallbackLogMapper paymentCallbackLogMapper;
    private final PaymentSignVerifier paymentSignVerifier;
    private final RedisGuardSupport redisGuardSupport;
    private final CourseLearningBizService courseLearningBizService;

    public PaymentCallbackBizService(PaymentRecordMapper paymentRecordMapper,
                                     OrderInfoMapper orderInfoMapper,
                                     CoachBookingMapper coachBookingMapper,
                                     PaymentCallbackLogMapper paymentCallbackLogMapper,
                                     PaymentSignVerifier paymentSignVerifier,
                                     RedisGuardSupport redisGuardSupport,
                                     CourseLearningBizService courseLearningBizService) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.coachBookingMapper = coachBookingMapper;
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
        this.paymentSignVerifier = paymentSignVerifier;
        this.redisGuardSupport = redisGuardSupport;
        this.courseLearningBizService = courseLearningBizService;
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

            String idempotencyKey = "CALLBACK_ASYNC_" + dto.getTradeNo();
            callbackGuardKey = RedisConstant.appPayCallbackPayNoGuardKey(paymentRecord.getPayNo());
            LockAcquireResult lockResult = redisGuardSupport.acquireLock(callbackGuardKey, RedisConstant.PAY_CALLBACK_GUARD_TTL_SEC);
            callbackLockToken = lockResult.token();
            if (lockResult.isBusy()) {
                log.setProcessResult("IN_FLIGHT");
                paymentCallbackLogMapper.insert(log);
                result = "success";
                return result;
            }

            LocalDateTime paidAt = LocalDateTime.now();
            int updated = paymentRecordMapper.markPaidIfUnpaid(paymentRecord.getId(), idempotencyKey, paidAt);
            if (updated == 0) {
                PaymentRecord latest = paymentRecordMapper.selectById(paymentRecord.getId());
                if (latest != null && BizStatusConstant.PayStatus.PAID.equals(latest.getPayStatus())) {
                    log.setProcessResult("IDEMPOTENT");
                    paymentCallbackLogMapper.insert(log);
                    result = "success";
                    return result;
                }
                log.setProcessResult("REJECTED");
                log.setErrorMessage("PAY_STATE_CONFLICT");
                paymentCallbackLogMapper.insert(log);
                result = "fail";
                return result;
            }

            OrderInfo orderInfo = orderInfoMapper.selectById(paymentRecord.getOrderId());
            if (orderInfo != null) {
                markOrderPaid(orderInfo);
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

    private void markOrderPaid(OrderInfo orderInfo) {
        if (BizStatusConstant.PayStatus.PAID.equals(orderInfo.getPayStatus())
                && BizStatusConstant.OrderStatus.PAID.equals(orderInfo.getOrderStatus())) {
            return;
        }
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
        orderInfoMapper.updateById(orderInfo);
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
            courseLearningBizService.syncEnrollmentPaid(orderInfo.getBizId());
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
