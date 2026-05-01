package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.redis.LockAcquireResult;
import com.huang.common.redis.RedisGuardSupport;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentCallbackLog;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.dto.pay.PayCallbackDTO;
import com.huang.web.app.mapper.CoachBookingMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.PaymentCallbackLogMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.service.pay.PaymentSignVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCallbackBizServiceTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private CoachBookingMapper coachBookingMapper;

    @Mock
    private PaymentCallbackLogMapper paymentCallbackLogMapper;

    @Mock
    private PaymentSignVerifier paymentSignVerifier;

    @Mock
    private RedisGuardSupport redisGuardSupport;

    @Mock
    private CourseLearningBizService courseLearningBizService;

    private PaymentCallbackBizService paymentCallbackBizService;

    @BeforeEach
    void setUp() {
        paymentCallbackBizService = new PaymentCallbackBizService(
                paymentRecordMapper,
                orderInfoMapper,
                coachBookingMapper,
                paymentCallbackLogMapper,
                paymentSignVerifier,
                redisGuardSupport,
                courseLearningBizService
        );
    }

    @Test
    void handleCallback_shouldReturnSuccessWhenSamePayNoIsAlreadyInFlight() {
        PayCallbackDTO dto = buildCallback();
        PaymentRecord paymentRecord = buildPaymentRecord();

        when(paymentSignVerifier.verify(dto)).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentRecord);
        when(redisGuardSupport.acquireLock(
                RedisConstant.appPayCallbackPayNoGuardKey("PAY001"),
                RedisConstant.PAY_CALLBACK_GUARD_TTL_SEC
        )).thenReturn(LockAcquireResult.busy());

        String result = paymentCallbackBizService.handleCallback(dto, "{\"payNo\":\"PAY001\"}");

        assertThat(result).isEqualTo("success");
        verify(paymentRecordMapper, never()).markPaidIfUnpaid(anyLong(), any(), any());
        ArgumentCaptor<PaymentCallbackLog> captor = ArgumentCaptor.forClass(PaymentCallbackLog.class);
        verify(paymentCallbackLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getProcessResult()).isEqualTo("IN_FLIGHT");
    }

    @Test
    void handleCallback_shouldProcessAndUpdateOrderWhenGuardAcquired() {
        PayCallbackDTO dto = buildCallback();
        PaymentRecord paymentRecord = buildPaymentRecord();
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(10L);
        orderInfo.setBizType(BizStatusConstant.BizType.COURSE_ENROLLMENT);
        orderInfo.setBizId(88L);

        when(paymentSignVerifier.verify(dto)).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentRecord);
        when(redisGuardSupport.acquireLock(
                RedisConstant.appPayCallbackPayNoGuardKey("PAY001"),
                RedisConstant.PAY_CALLBACK_GUARD_TTL_SEC
        )).thenReturn(LockAcquireResult.acquired("lock-token"));
        when(paymentRecordMapper.markPaidIfUnpaid(eq(1L), eq("CALLBACK_ASYNC_TRADE001"), any())).thenReturn(1);
        when(orderInfoMapper.selectById(10L)).thenReturn(orderInfo);
        when(courseLearningBizService.syncEnrollmentPaid(88L)).thenReturn(true);

        String result = paymentCallbackBizService.handleCallback(dto, "{\"payNo\":\"PAY001\"}");

        assertThat(result).isEqualTo("success");
        verify(paymentRecordMapper).markPaidIfUnpaid(eq(1L), eq("CALLBACK_ASYNC_TRADE001"), any());
        verify(orderInfoMapper).updateById(any(OrderInfo.class));
        verify(courseLearningBizService).syncEnrollmentPaid(88L);
        verify(paymentCallbackLogMapper).insert(any(PaymentCallbackLog.class));
        verify(redisGuardSupport).releaseLock(
                RedisConstant.appPayCallbackPayNoGuardKey("PAY001"),
                "lock-token"
        );
    }

    @Test
    void handleCallback_shouldReturnIdempotentWhenLockDegradedAndPaymentAlreadyPaid() {
        PayCallbackDTO dto = buildCallback();
        PaymentRecord paymentRecord = buildPaymentRecord();
        PaymentRecord latest = buildPaymentRecord();
        latest.setPayStatus(BizStatusConstant.PayStatus.PAID);

        when(paymentSignVerifier.verify(dto)).thenReturn(true);
        when(paymentRecordMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(paymentRecord);
        when(redisGuardSupport.acquireLock(
                RedisConstant.appPayCallbackPayNoGuardKey("PAY001"),
                RedisConstant.PAY_CALLBACK_GUARD_TTL_SEC
        )).thenReturn(LockAcquireResult.degraded());
        when(paymentRecordMapper.markPaidIfUnpaid(eq(1L), eq("CALLBACK_ASYNC_TRADE001"), any())).thenReturn(0);
        when(paymentRecordMapper.selectById(1L)).thenReturn(latest);

        String result = paymentCallbackBizService.handleCallback(dto, "{\"payNo\":\"PAY001\"}");

        assertThat(result).isEqualTo("success");
        ArgumentCaptor<PaymentCallbackLog> captor = ArgumentCaptor.forClass(PaymentCallbackLog.class);
        verify(paymentCallbackLogMapper).insert(captor.capture());
        assertThat(captor.getValue().getProcessResult()).isEqualTo("IDEMPOTENT");
        verify(paymentRecordMapper).markPaidIfUnpaid(eq(1L), eq("CALLBACK_ASYNC_TRADE001"), any());
    }

    private PayCallbackDTO buildCallback() {
        PayCallbackDTO dto = new PayCallbackDTO();
        dto.setPayNo("PAY001");
        dto.setTradeNo("TRADE001");
        dto.setStatus("SUCCESS");
        dto.setAmount(new BigDecimal("99.00"));
        dto.setSign("MOCK");
        return dto;
    }

    private PaymentRecord buildPaymentRecord() {
        PaymentRecord record = new PaymentRecord();
        record.setId(1L);
        record.setOrderId(10L);
        record.setPayNo("PAY001");
        record.setPayAmount(new BigDecimal("99.00"));
        record.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        return record;
    }
}
