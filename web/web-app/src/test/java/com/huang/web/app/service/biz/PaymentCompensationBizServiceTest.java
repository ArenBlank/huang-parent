package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.mapper.CoachBookingMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCompensationBizServiceTest {

    @Mock
    private PaymentRecordMapper paymentRecordMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private CoachBookingMapper coachBookingMapper;

    @Mock
    private CourseLearningBizService courseLearningBizService;

    private PaymentCompensationBizService paymentCompensationBizService;

    @BeforeEach
    void setUp() {
        paymentCompensationBizService = new PaymentCompensationBizService(
                paymentRecordMapper,
                orderInfoMapper,
                coachBookingMapper,
                courseLearningBizService
        );
    }

    @Test
    void repairPaidOrders_shouldSyncCourseEnrollmentCheckInState() {
        PaymentRecord record = new PaymentRecord();
        record.setOrderId(10L);
        record.setPayStatus(BizStatusConstant.PayStatus.PAID);
        record.setPayAmount(new BigDecimal("19.90"));

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(10L);
        orderInfo.setBizType(BizStatusConstant.BizType.COURSE_ENROLLMENT);
        orderInfo.setBizId(88L);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);

        when(paymentRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(record));
        when(orderInfoMapper.selectById(10L)).thenReturn(orderInfo);
        when(courseLearningBizService.syncEnrollmentPaid(88L)).thenReturn(true);

        int fixed = paymentCompensationBizService.repairPaidOrders(20);

        assertThat(fixed).isEqualTo(1);
        verify(courseLearningBizService).syncEnrollmentPaid(88L);
    }
}
