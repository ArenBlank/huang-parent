package com.huang.web.app.service.biz;

import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.CoachSchedule;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.dto.booking.CreateBookingDTO;
import com.huang.web.app.mapper.CoachBookingMapper;
import com.huang.web.app.mapper.CoachProfileMapper;
import com.huang.web.app.mapper.CoachReviewMapper;
import com.huang.web.app.mapper.CoachScheduleMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.OrderItemMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingBizServiceTest {

    @Mock
    private CoachScheduleMapper coachScheduleMapper;

    @Mock
    private CoachBookingMapper coachBookingMapper;

    @Mock
    private CoachProfileMapper coachProfileMapper;

    @Mock
    private CoachReviewMapper coachReviewMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private UserService userService;

    private BookingBizService bookingBizService;

    @BeforeEach
    void setUp() {
        bookingBizService = new BookingBizService(
                coachScheduleMapper,
                coachBookingMapper,
                coachProfileMapper,
                coachReviewMapper,
                orderInfoMapper,
                orderItemMapper,
                paymentRecordMapper,
                userService
        );
    }

    @Test
    void createBooking_shouldReturnNullWhenUniqueIndexFallbackTriggers() {
        CreateBookingDTO dto = new CreateBookingDTO();
        dto.setScheduleId(5L);

        CoachSchedule schedule = new CoachSchedule();
        schedule.setId(5L);
        schedule.setCoachId(9L);
        schedule.setStatus(1);
        schedule.setPrice(new BigDecimal("199.00"));
        schedule.setScheduleDate(LocalDate.now().plusDays(1));
        schedule.setStartTime(LocalTime.of(10, 0));
        schedule.setEndTime(LocalTime.of(11, 0));

        when(coachScheduleMapper.selectById(5L)).thenReturn(schedule);
        when(coachBookingMapper.countAnyByUserAndSchedule(7L, 5L)).thenReturn(0L);
        when(coachScheduleMapper.reserveSlot(5L)).thenReturn(1);
        doThrow(new DuplicateKeyException("uk_coach_booking")).when(coachBookingMapper).insert(any());

        var result = bookingBizService.createBooking(7L, dto);

        assertThat(result).isNull();
        verify(orderInfoMapper).insert(any(OrderInfo.class));
        verify(paymentRecordMapper).insert(any());
    }

    @Test
    void cancelUnpaid_shouldCloseOrderAndReleaseSlot() {
        CoachBooking booking = new CoachBooking();
        booking.setId(9L);
        booking.setUserId(7L);
        booking.setScheduleId(5L);
        booking.setOrderId(11L);
        booking.setBookingStatus(BizStatusConstant.BookingStatus.WAIT_PAY);
        booking.setPayStatus(BizStatusConstant.PayStatus.UNPAID);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(11L);
        orderInfo.setUserId(7L);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.NEW);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.UNPAID);

        when(coachBookingMapper.selectById(9L)).thenReturn(booking);
        when(orderInfoMapper.selectById(11L)).thenReturn(orderInfo);

        boolean result = bookingBizService.cancelUnpaid(9L, 7L);

        assertThat(result).isTrue();
        assertThat(orderInfo.getOrderStatus()).isEqualTo(BizStatusConstant.OrderStatus.CLOSED);
        assertThat(orderInfo.getPayStatus()).isEqualTo(BizStatusConstant.PayStatus.CLOSED);
        verify(orderInfoMapper).updateById(orderInfo);
        verify(paymentRecordMapper).update(any(), any());
        verify(coachScheduleMapper).releaseSlot(5L);
        verify(coachBookingMapper).deleteById(9L);
    }

    @Test
    void markPaySuccess_shouldTreatPaidRecordAsIdempotentAndSyncOrderAndBooking() {
        CoachBooking booking = new CoachBooking();
        booking.setId(12L);
        booking.setUserId(7L);
        booking.setOrderId(33L);
        booking.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        booking.setBookingStatus(BizStatusConstant.BookingStatus.WAIT_PAY);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(33L);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.NEW);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setId(77L);
        paymentRecord.setOrderId(33L);
        paymentRecord.setPayNo("BPAY001");

        PaymentRecord latest = new PaymentRecord();
        latest.setId(77L);
        latest.setPayStatus(BizStatusConstant.PayStatus.PAID);

        when(coachBookingMapper.selectById(12L)).thenReturn(booking);
        when(orderInfoMapper.selectById(33L)).thenReturn(orderInfo);
        when(paymentRecordMapper.selectOne(any())).thenReturn(paymentRecord);
        when(paymentRecordMapper.markPaidIfUnpaid(eq(77L), eq("CALLBACK_BPAY001"), any())).thenReturn(0);
        when(paymentRecordMapper.selectById(77L)).thenReturn(latest);

        boolean ok = bookingBizService.markPaySuccess(12L, 7L);

        assertThat(ok).isTrue();
        verify(paymentRecordMapper).markPaidIfUnpaid(eq(77L), eq("CALLBACK_BPAY001"), any());
        verify(coachBookingMapper).updateById(booking);
        verify(orderInfoMapper).updateById(orderInfo);
    }
}
