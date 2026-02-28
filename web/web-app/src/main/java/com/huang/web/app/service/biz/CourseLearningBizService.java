package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.model.entity.Course;
import com.huang.model.entity.CourseEnrollment;
import com.huang.model.entity.CourseSchedule;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.OrderItem;
import com.huang.model.entity.PaymentRecord;
import com.huang.model.entity.RefundRecord;
import com.huang.web.app.dto.course.CourseEnrollDTO;
import com.huang.web.app.mapper.CourseEnrollmentMapper;
import com.huang.web.app.mapper.CourseMapper;
import com.huang.web.app.mapper.CourseScheduleMapper;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.OrderItemMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.mapper.RefundRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CourseLearningBizService {

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;

    public CourseLearningBizService(CourseMapper courseMapper,
                                    CourseScheduleMapper courseScheduleMapper,
                                    CourseEnrollmentMapper courseEnrollmentMapper,
                                    OrderInfoMapper orderInfoMapper,
                                    OrderItemMapper orderItemMapper,
                                    PaymentRecordMapper paymentRecordMapper,
                                    RefundRecordMapper refundRecordMapper) {
        this.courseMapper = courseMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundRecordMapper = refundRecordMapper;
    }

    public List<Course> listCourses(Long categoryId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 1)
                .orderByDesc(Course::getId);
        if (categoryId != null) {
            wrapper.eq(Course::getCategoryId, categoryId);
        }
        return courseMapper.selectList(wrapper);
    }

    public List<CourseSchedule> listSchedules(Long courseId) {
        return courseScheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCourseId, courseId)
                        .eq(CourseSchedule::getStatus, 1)
                        .orderByAsc(CourseSchedule::getStartTime)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> enroll(Long userId, CourseEnrollDTO dto) {
        CourseSchedule schedule = courseScheduleMapper.selectById(dto.getScheduleId());
        if (schedule == null || schedule.getStatus() == null || schedule.getStatus() != 1) {
            return null;
        }
        Course course = courseMapper.selectById(schedule.getCourseId());
        if (course == null || course.getStatus() == null || course.getStatus() != 1) {
            return null;
        }

        int updated = courseScheduleMapper.update(
                null,
                new LambdaUpdateWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getId, schedule.getId())
                        .eq(CourseSchedule::getStatus, 1)
                        .apply("booked_count < capacity")
                        .setSql("booked_count = booked_count + 1")
        );
        if (updated == 0) {
            return null;
        }

        CourseEnrollment existed = courseEnrollmentMapper.selectOne(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getUserId, userId)
                        .eq(CourseEnrollment::getCourseId, course.getId())
                        .eq(CourseEnrollment::getScheduleId, schedule.getId())
                        .last("LIMIT 1")
        );
        if (existed != null) {
            return null;
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo(genNo("CRS"));
        orderInfo.setUserId(userId);
        orderInfo.setTotalAmount(course.getPrice() == null ? BigDecimal.ZERO : course.getPrice());
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.UNPAID);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        orderInfo.setBizType(BizStatusConstant.BizType.COURSE_ENROLLMENT);
        orderInfoMapper.insert(orderInfo);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderInfo.getId());
        orderItem.setItemType("course");
        orderItem.setItemId(course.getId());
        orderItem.setItemName(course.getTitle());
        orderItem.setPrice(orderInfo.getTotalAmount());
        orderItem.setQuantity(1);
        orderItem.setAmount(orderInfo.getTotalAmount());
        orderItemMapper.insert(orderItem);

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setOrderId(orderInfo.getId());
        paymentRecord.setPayNo(genNo("CPAY"));
        paymentRecord.setPayChannel("wechat");
        paymentRecord.setPayAmount(orderInfo.getTotalAmount());
        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.UNPAID);
        paymentRecordMapper.insert(paymentRecord);

        CourseEnrollment enrollment = new CourseEnrollment();
        enrollment.setUserId(userId);
        enrollment.setCourseId(course.getId());
        enrollment.setScheduleId(schedule.getId());
        enrollment.setOrderId(orderInfo.getId());
        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.UNPAID);
        enrollment.setEnrollTime(LocalDateTime.now());
        courseEnrollmentMapper.insert(enrollment);

        orderInfo.setBizId(enrollment.getId());
        orderInfoMapper.updateById(orderInfo);

        Map<String, Object> result = new HashMap<>();
        result.put("enrollmentId", enrollment.getId());
        result.put("orderId", orderInfo.getId());
        result.put("orderNo", orderInfo.getOrderNo());
        result.put("payNo", paymentRecord.getPayNo());
        result.put("amount", orderInfo.getTotalAmount());
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaySuccess(Long enrollmentId, Long userId) {
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
        if (enrollment == null || !enrollment.getUserId().equals(userId)) {
            return false;
        }
        OrderInfo orderInfo = orderInfoMapper.selectById(enrollment.getOrderId());
        if (orderInfo == null) {
            return false;
        }
        PaymentRecord paymentRecord = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderInfo.getId())
                        .last("LIMIT 1")
        );
        if (paymentRecord == null) {
            return false;
        }

        String idempotencyKey = "COURSE_CALLBACK_" + paymentRecord.getPayNo();
        if (idempotencyKey.equals(paymentRecord.getCallbackIdempotencyKey())
                || BizStatusConstant.PayStatus.PAID.equals(paymentRecord.getPayStatus())) {
            return true;
        }

        orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
        orderInfoMapper.updateById(orderInfo);

        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.PAID);
        paymentRecord.setPayTime(LocalDateTime.now());
        paymentRecord.setCallbackIdempotencyKey(idempotencyKey);
        paymentRecordMapper.updateById(paymentRecord);
        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.PAID);
        courseEnrollmentMapper.updateById(enrollment);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelUnpaid(Long enrollmentId, Long userId) {
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
        if (enrollment == null || !enrollment.getUserId().equals(userId)) {
            return false;
        }

        OrderInfo orderInfo = orderInfoMapper.selectById(enrollment.getOrderId());
        if (orderInfo == null) {
            return false;
        }
        if (BizStatusConstant.OrderStatus.CLOSED.equals(orderInfo.getOrderStatus())) {
            return true;
        }
        if (!BizStatusConstant.PayStatus.UNPAID.equals(orderInfo.getPayStatus())) {
            return false;
        }

        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.CLOSED);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.CLOSED);
        orderInfoMapper.updateById(orderInfo);

        paymentRecordMapper.update(
                null,
                new LambdaUpdateWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderInfo.getId())
                        .set(PaymentRecord::getPayStatus, BizStatusConstant.PayStatus.CLOSED)
        );

        courseScheduleMapper.update(
                null,
                new LambdaUpdateWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getId, enrollment.getScheduleId())
                        .gt(CourseSchedule::getBookedCount, 0)
                        .setSql("booked_count = booked_count - 1")
        );

        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.CANCELED);
        courseEnrollmentMapper.updateById(enrollment);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean refundPaid(Long enrollmentId, Long userId, String reason) {
        if (reason == null || reason.trim().isEmpty() || reason.trim().length() > 200) {
            return false;
        }
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
        if (enrollment == null || !enrollment.getUserId().equals(userId)) {
            return false;
        }
        OrderInfo orderInfo = orderInfoMapper.selectById(enrollment.getOrderId());
        if (orderInfo == null) {
            return false;
        }
        if (BizStatusConstant.OrderStatus.REFUNDED.equals(orderInfo.getOrderStatus())) {
            return true;
        }
        if (!BizStatusConstant.PayStatus.PAID.equals(orderInfo.getPayStatus())) {
            return false;
        }

        PaymentRecord paymentRecord = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderInfo.getId())
                        .last("LIMIT 1")
        );
        if (paymentRecord == null) {
            return false;
        }

        orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.REFUNDED);
        orderInfo.setPayStatus(BizStatusConstant.PayStatus.REFUNDED);
        orderInfoMapper.updateById(orderInfo);

        paymentRecord.setPayStatus(BizStatusConstant.PayStatus.REFUNDED);
        paymentRecordMapper.updateById(paymentRecord);

        courseScheduleMapper.update(
                null,
                new LambdaUpdateWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getId, enrollment.getScheduleId())
                        .gt(CourseSchedule::getBookedCount, 0)
                        .setSql("booked_count = booked_count - 1")
        );

        RefundRecord refundRecord = new RefundRecord();
        refundRecord.setOrderId(orderInfo.getId());
        refundRecord.setRefundNo(genNo("RFD"));
        refundRecord.setRefundAmount(orderInfo.getTotalAmount());
        refundRecord.setRefundStatus(BizStatusConstant.RefundStatus.REFUNDED);
        refundRecord.setRefundTime(LocalDateTime.now());
        refundRecord.setReason(reason.trim());
        refundRecordMapper.insert(refundRecord);

        enrollment.setStatus(BizStatusConstant.EnrollmentStatus.REFUNDED);
        courseEnrollmentMapper.updateById(enrollment);
        return true;
    }

    public List<CourseEnrollment> myEnrollments(Long userId) {
        return courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getUserId, userId)
                        .orderByDesc(CourseEnrollment::getId)
        );
    }

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().toString().replace("-", "").replace(":", "").replace("T", "").replace(".", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}
