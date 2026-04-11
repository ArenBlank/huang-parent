package com.huang.web.app.service.biz;

import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.model.entity.Course;
import com.huang.model.entity.CourseSchedule;
import com.huang.model.entity.OrderInfo;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
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
}
