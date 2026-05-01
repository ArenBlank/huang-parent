package com.huang.web.app.service.biz;

import com.huang.common.constant.BizStatusConstant;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.Course;
import com.huang.model.entity.CourseEnrollment;
import com.huang.model.entity.CourseSchedule;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.dto.course.CourseEnrollDTO;
import com.huang.web.app.mapper.CourseEnrollmentMapper;
import com.huang.web.app.mapper.CourseMapper;
import com.huang.web.app.mapper.CourseScheduleMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.OrderItemMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.mapper.RefundRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseLearningBizServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private CourseScheduleMapper courseScheduleMapper;

    @Mock
    private CourseEnrollmentMapper courseEnrollmentMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private OrderItemMapper orderItemMapper;

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private RefundRecordMapper refundRecordMapper;

    @Mock
    private MultiLevelCacheSupport multiLevelCacheSupport;

    private CourseLearningBizService courseLearningBizService;

    @BeforeEach
    void setUp() {
        courseLearningBizService = new CourseLearningBizService(
                courseMapper,
                courseScheduleMapper,
                courseEnrollmentMapper,
                orderInfoMapper,
                orderItemMapper,
                paymentRecordMapper,
                refundRecordMapper,
                multiLevelCacheSupport
        );
    }

    @Test
    void enroll_shouldReturnNullWhenUniqueIndexFallbackTriggers() {
        CourseEnrollDTO dto = new CourseEnrollDTO();
        dto.setScheduleId(3L);

        CourseSchedule schedule = new CourseSchedule();
        schedule.setId(3L);
        schedule.setCourseId(8L);
        schedule.setStatus(1);

        Course course = new Course();
        course.setId(8L);
        course.setStatus(1);
        course.setPrice(new BigDecimal("59.90"));
        course.setTitle("Core Training");

        when(courseScheduleMapper.selectById(3L)).thenReturn(schedule);
        when(courseMapper.selectById(8L)).thenReturn(course);
        when(courseEnrollmentMapper.selectOne(any())).thenReturn(null);
        when(courseScheduleMapper.update(any(), any())).thenReturn(1);
        doThrow(new DuplicateKeyException("uk_course_enrollment")).when(courseEnrollmentMapper).insert(any());

        var result = courseLearningBizService.enroll(11L, dto);

        assertThat(result).isNull();
        verify(orderInfoMapper).insert(any(OrderInfo.class));
        verify(paymentRecordMapper).insert(any());
    }

    @Test
    void markPaySuccess_shouldGenerateCheckInCodeAndMoveAttendStatusToWaitClass() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(5L);
        enrollment.setUserId(9L);
        enrollment.setOrderId(44L);
        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.UNPAID);
        enrollment.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(44L);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.UNPAID);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setId(77L);
        paymentRecord.setOrderId(44L);
        paymentRecord.setPayNo("CPAY001");
        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.UNPAID);

        when(courseEnrollmentMapper.selectById(5L)).thenReturn(enrollment, enrollment);
        when(orderInfoMapper.selectById(44L)).thenReturn(orderInfo);
        when(paymentRecordMapper.selectOne(any())).thenReturn(paymentRecord);
        when(paymentRecordMapper.markPaidIfUnpaid(eq(77L), eq("COURSE_CALLBACK_CPAY001"), any())).thenReturn(1);

        boolean ok = courseLearningBizService.markPaySuccess(5L, 9L);

        assertThat(ok).isTrue();
        verify(orderInfoMapper).updateById(orderInfo);
        verify(paymentRecordMapper).markPaidIfUnpaid(eq(77L), eq("COURSE_CALLBACK_CPAY001"), any());

        ArgumentCaptor<CourseEnrollment> captor = ArgumentCaptor.forClass(CourseEnrollment.class);
        verify(courseEnrollmentMapper, atLeast(2)).updateById(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(item ->
                item.getId().equals(5L)
                        && Integer.valueOf(BizStatusConstant.EnrollmentStatus.PAID).equals(item.getStatus()));
        assertThat(captor.getAllValues()).anyMatch(item ->
                item.getId().equals(5L)
                        && Integer.valueOf(BizStatusConstant.AttendStatus.WAIT_CLASS).equals(item.getAttendStatus())
                        && item.getCheckInCode() != null
                        && item.getCheckInCode().length() == 6);
    }

    @Test
    void markPaySuccess_shouldTreatPaidRecordAsIdempotentAndOnlySyncEnrollment() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(6L);
        enrollment.setUserId(9L);
        enrollment.setOrderId(45L);
        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.UNPAID);
        enrollment.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(45L);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setId(78L);
        paymentRecord.setOrderId(45L);
        paymentRecord.setPayNo("CPAY002");

        PaymentRecord latest = new PaymentRecord();
        latest.setId(78L);
        latest.setPayStatus(BizStatusConstant.PayStatus.PAID);

        when(courseEnrollmentMapper.selectById(6L)).thenReturn(enrollment, enrollment);
        when(orderInfoMapper.selectById(45L)).thenReturn(orderInfo);
        when(paymentRecordMapper.selectOne(any())).thenReturn(paymentRecord);
        when(paymentRecordMapper.markPaidIfUnpaid(eq(78L), eq("COURSE_CALLBACK_CPAY002"), any())).thenReturn(0);
        when(paymentRecordMapper.selectById(78L)).thenReturn(latest);

        boolean ok = courseLearningBizService.markPaySuccess(6L, 9L);

        assertThat(ok).isTrue();
        verify(orderInfoMapper, never()).updateById(orderInfo);
        verify(paymentRecordMapper).markPaidIfUnpaid(eq(78L), eq("COURSE_CALLBACK_CPAY002"), any());
        verify(courseEnrollmentMapper, atLeast(2)).updateById(any(CourseEnrollment.class));
    }

    @Test
    void refundPaid_shouldSetAttendStatusInvalid() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(8L);
        enrollment.setUserId(11L);
        enrollment.setOrderId(66L);
        enrollment.setScheduleId(99L);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(66L);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
        orderInfo.setTotalAmount(new BigDecimal("88.00"));

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(66L);
        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.PAID);

        when(courseEnrollmentMapper.selectById(8L)).thenReturn(enrollment);
        when(orderInfoMapper.selectById(66L)).thenReturn(orderInfo);
        when(paymentRecordMapper.selectOne(any())).thenReturn(paymentRecord);

        boolean ok = courseLearningBizService.refundPaid(8L, 11L, "行程冲突");

        assertThat(ok).isTrue();
        verify(courseEnrollmentMapper).updateById(argThat(item ->
                item.getId().equals(8L)
                        && Integer.valueOf(BizStatusConstant.EnrollmentStatus.REFUNDED).equals(item.getStatus())
                        && Integer.valueOf(BizStatusConstant.AttendStatus.INVALID).equals(item.getAttendStatus())));
    }

    @Test
    void checkInByCode_shouldTreatAlreadyCheckedInAsIdempotentSuccess() {
        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setId(18L);
        enrollment.setUserId(7L);
        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.PAID);
        enrollment.setAttendStatus(BizStatusConstant.AttendStatus.CHECKED_IN);
        enrollment.setCheckInCode("ABC123");

        when(courseEnrollmentMapper.selectOne(any())).thenReturn(enrollment);

        boolean ok = courseLearningBizService.checkInByCode(7L, "abc123");

        assertThat(ok).isTrue();
        verify(courseEnrollmentMapper, never()).updateById(any());
    }
}
