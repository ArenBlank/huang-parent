package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.CoachBooking;
import com.huang.model.entity.CourseEnrollment;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.mapper.CoachBookingMapper;
import com.huang.web.app.mapper.CourseEnrollmentMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentCompensationBizService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompensationBizService.class);

    private final PaymentRecordMapper paymentRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final CoachBookingMapper coachBookingMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;

    public PaymentCompensationBizService(PaymentRecordMapper paymentRecordMapper,
                                         OrderInfoMapper orderInfoMapper,
                                         CoachBookingMapper coachBookingMapper,
                                         CourseEnrollmentMapper courseEnrollmentMapper) {
        this.paymentRecordMapper = paymentRecordMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.coachBookingMapper = coachBookingMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
    }

    @Transactional(rollbackFor = Exception.class)
    public int repairPaidOrders(int limit) {
        if (limit <= 0) {
            return 0;
        }
        List<PaymentRecord> paidRecords = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getPayStatus, BizStatusConstant.PayStatus.PAID)
                        .orderByDesc(PaymentRecord::getPayTime)
                        .last("LIMIT " + limit)
        );
        if (paidRecords == null || paidRecords.isEmpty()) {
            return 0;
        }
        int fixed = 0;
        for (PaymentRecord record : paidRecords) {
            OrderInfo orderInfo = orderInfoMapper.selectById(record.getOrderId());
            if (orderInfo == null) {
                continue;
            }
            if (BizStatusConstant.PayStatus.REFUNDED.equals(orderInfo.getPayStatus())
                    || BizStatusConstant.PayStatus.CLOSED.equals(orderInfo.getPayStatus())
                    || BizStatusConstant.OrderStatus.REFUNDED.equals(orderInfo.getOrderStatus())
                    || BizStatusConstant.OrderStatus.CLOSED.equals(orderInfo.getOrderStatus())) {
                continue;
            }
            boolean updated = false;
            if (!BizStatusConstant.PayStatus.PAID.equals(orderInfo.getPayStatus())
                    || !BizStatusConstant.OrderStatus.PAID.equals(orderInfo.getOrderStatus())) {
                orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
                orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
                orderInfoMapper.updateById(orderInfo);
                updated = true;
            }
            boolean bizUpdated = repairBizStatus(orderInfo);
            if (updated || bizUpdated) {
                fixed++;
            }
        }
        log.info("PAY_COMPENSATE fixed={} scanned={}", fixed, paidRecords.size());
        return fixed;
    }

    private boolean repairBizStatus(OrderInfo orderInfo) {
        if (BizStatusConstant.BizType.COACH_BOOKING.equals(orderInfo.getBizType())) {
            CoachBooking booking = coachBookingMapper.selectById(orderInfo.getBizId());
            if (booking != null) {
                boolean changed = false;
                if (!BizStatusConstant.PayStatus.PAID.equals(booking.getPayStatus())) {
                    booking.setPayStatus(BizStatusConstant.PayStatus.PAID);
                    changed = true;
                }
                if (!BizStatusConstant.BookingStatus.PAID.equals(booking.getBookingStatus())) {
                    booking.setBookingStatus(BizStatusConstant.BookingStatus.PAID);
                    changed = true;
                }
                if (changed) {
                    coachBookingMapper.updateById(booking);
                }
                return changed;
            }
            return false;
        }

        String bizType = orderInfo.getBizType() == null ? "" : orderInfo.getBizType();
        if (BizStatusConstant.BizType.COURSE_ENROLLMENT.equals(bizType)
                || BizStatusConstant.BizType.COURSE.equals(bizType)) {
            CourseEnrollment enrollment = courseEnrollmentMapper.selectById(orderInfo.getBizId());
            if (enrollment != null && enrollment.getStatus() != BizStatusConstant.EnrollmentStatus.PAID) {
                enrollment.setStatus(BizStatusConstant.EnrollmentStatus.PAID);
                courseEnrollmentMapper.updateById(enrollment);
                return true;
            }
        }
        return false;
    }
}
