package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.exception.HuangException;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.CodeUtil;
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
import com.huang.web.app.vo.course.CourseEnrollmentHistoryVO;
import com.huang.web.app.vo.course.CourseMyScheduleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CourseLearningBizService {

    private static final Logger log = LoggerFactory.getLogger(CourseLearningBizService.class);
    private static final DateTimeFormatter SQL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int CHECK_IN_CODE_LENGTH = 6;
    private static final int CHECK_IN_CODE_MAX_RETRY = 10;
    private static final String CHECK_IN_CODE_GENERATE_FAILED_MESSAGE = "核销码生成失败，请稍后再试";

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    public CourseLearningBizService(CourseMapper courseMapper,
                                    CourseScheduleMapper courseScheduleMapper,
                                    CourseEnrollmentMapper courseEnrollmentMapper,
                                    OrderInfoMapper orderInfoMapper,
                                    OrderItemMapper orderItemMapper,
                                    PaymentRecordMapper paymentRecordMapper,
                                    RefundRecordMapper refundRecordMapper,
                                    MultiLevelCacheSupport multiLevelCacheSupport) {
        this.courseMapper = courseMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundRecordMapper = refundRecordMapper;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Course> listCourses(Long categoryId) {
        String cacheKey = RedisConstant.appCourseListKey(categoryId);
        var cached = multiLevelCacheSupport.getJson(cacheKey, new TypeReference<List<Course>>() {});
        if (cached.found()) {
            return cached.nullValue() ? List.of() : cached.value();
        }

        String nowText = LocalDateTime.now().format(SQL_DATE_TIME_FORMATTER);
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>()
                .eq(Course::getStatus, 1)
                .inSql(
                        Course::getId,
                        "SELECT DISTINCT course_id FROM course_schedule " +
                                "WHERE status = 1 AND is_deleted = 0 AND end_time >= '" + nowText + "'"
                )
                .orderByDesc(Course::getId);
        if (categoryId != null) {
            wrapper.eq(Course::getCategoryId, categoryId);
        }
        List<Course> courses = courseMapper.selectList(wrapper);
        multiLevelCacheSupport.setJson(
                cacheKey,
                courses,
                multiLevelCacheSupport.ttlWithJitter(RedisConstant.APP_COURSE_LIST_TTL_SEC, RedisConstant.JITTER_SHORT_SEC)
        );
        return courses;
    }

    public List<CourseSchedule> listSchedules(Long courseId) {
        LocalDateTime now = LocalDateTime.now();
        return courseScheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCourseId, courseId)
                        .eq(CourseSchedule::getStatus, 1)
                        .ge(CourseSchedule::getEndTime, now)
                        .orderByAsc(CourseSchedule::getStartTime)
        );
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> enroll(Long userId, CourseEnrollDTO dto) {
        long start = System.currentTimeMillis();
        Long enrollmentId = null;
        boolean success = false;
        String reason = null;
        try {
            CourseSchedule schedule = courseScheduleMapper.selectById(dto.getScheduleId());
            if (schedule == null || schedule.getStatus() == null || schedule.getStatus() != 1) {
                reason = "schedule_invalid";
                return null;
            }
            Course course = courseMapper.selectById(schedule.getCourseId());
            if (course == null || course.getStatus() == null || course.getStatus() != 1) {
                reason = "course_invalid";
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
                reason = "already_enrolled";
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
                reason = "schedule_full";
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
            enrollment.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);
            enrollment.setEnrollTime(LocalDateTime.now());
            courseEnrollmentMapper.insert(enrollment);

            enrollmentId = enrollment.getId();
            orderInfo.setBizId(enrollmentId);
            orderInfoMapper.updateById(orderInfo);

            Map<String, Object> result = new HashMap<>();
            result.put("enrollmentId", enrollmentId);
            result.put("orderId", orderInfo.getId());
            result.put("orderNo", orderInfo.getOrderNo());
            result.put("payNo", paymentRecord.getPayNo());
            result.put("amount", orderInfo.getTotalAmount());
            success = true;
            return result;
        } catch (DataIntegrityViolationException e) {
            markCurrentTransactionRollbackOnly();
            reason = "duplicate_enrollment_fallback";
            log.info("COURSE_ENROLL duplicate fallback triggered, userId={} scheduleId={} message={}",
                    userId,
                    dto == null ? null : dto.getScheduleId(),
                    e.getMessage());
            return null;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            log.info("COURSE_ENROLL userId={} scheduleId={} success={} enrollmentId={} reason={} costMs={}",
                    userId,
                    dto == null ? null : dto.getScheduleId(),
                    success,
                    enrollmentId,
                    reason,
                    costMs);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean markPaySuccess(Long enrollmentId, Long userId) {
        long start = System.currentTimeMillis();
        boolean success = false;
        try {
            CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
            if (enrollment == null || !Objects.equals(enrollment.getUserId(), userId)) {
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
                success = syncEnrollmentPaid(enrollment.getId());
                return success;
            }

            orderInfo.setPayStatus(BizStatusConstant.PayStatus.PAID);
            orderInfo.setOrderStatus(BizStatusConstant.OrderStatus.PAID);
            orderInfoMapper.updateById(orderInfo);

            paymentRecord.setPayStatus(BizStatusConstant.PayStatus.PAID);
            paymentRecord.setPayTime(LocalDateTime.now());
            paymentRecord.setCallbackIdempotencyKey(idempotencyKey);
            paymentRecordMapper.updateById(paymentRecord);
            success = syncEnrollmentPaid(enrollment.getId());
            return success;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            log.info("COURSE_PAY_SUCCESS userId={} enrollmentId={} success={} costMs={}",
                    userId, enrollmentId, success, costMs);
        }
    }

    public boolean syncEnrollmentPaid(Long enrollmentId) {
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
        if (enrollment == null) {
            return false;
        }
        if (!Objects.equals(enrollment.getStatus(), BizStatusConstant.EnrollmentStatus.PAID)) {
            CourseEnrollment patch = new CourseEnrollment();
            patch.setId(enrollment.getId());
            patch.setStatus(BizStatusConstant.EnrollmentStatus.PAID);
            courseEnrollmentMapper.updateById(patch);
            enrollment.setStatus(BizStatusConstant.EnrollmentStatus.PAID);
        }
        ensureCheckInReady(enrollment);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean cancelUnpaid(Long enrollmentId, Long userId) {
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
        if (enrollment == null || !Objects.equals(enrollment.getUserId(), userId)) {
            return false;
        }

        OrderInfo orderInfo = orderInfoMapper.selectById(enrollment.getOrderId());
        if (orderInfo == null) {
            return false;
        }
        if (BizStatusConstant.OrderStatus.CLOSED.equals(orderInfo.getOrderStatus())) {
            if (!Objects.equals(enrollment.getStatus(), BizStatusConstant.EnrollmentStatus.CANCELED)
                    || !Objects.equals(enrollment.getAttendStatus(), BizStatusConstant.AttendStatus.INVALID)) {
                CourseEnrollment patch = new CourseEnrollment();
                patch.setId(enrollment.getId());
                patch.setStatus(BizStatusConstant.EnrollmentStatus.CANCELED);
                patch.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);
                courseEnrollmentMapper.updateById(patch);
            }
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

        CourseEnrollment patch = new CourseEnrollment();
        patch.setId(enrollment.getId());
        patch.setStatus(BizStatusConstant.EnrollmentStatus.CANCELED);
        patch.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);
        courseEnrollmentMapper.updateById(patch);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean refundPaid(Long enrollmentId, Long userId, String reason) {
        long start = System.currentTimeMillis();
        boolean success = false;
        try {
            if (reason == null || reason.trim().isEmpty() || reason.trim().length() > 200) {
                return false;
            }
            CourseEnrollment enrollment = courseEnrollmentMapper.selectById(enrollmentId);
            if (enrollment == null || !Objects.equals(enrollment.getUserId(), userId)) {
                return false;
            }
            OrderInfo orderInfo = orderInfoMapper.selectById(enrollment.getOrderId());
            if (orderInfo == null) {
                return false;
            }
            if (BizStatusConstant.OrderStatus.REFUNDED.equals(orderInfo.getOrderStatus())) {
                if (!Objects.equals(enrollment.getStatus(), BizStatusConstant.EnrollmentStatus.REFUNDED)
                        || !Objects.equals(enrollment.getAttendStatus(), BizStatusConstant.AttendStatus.INVALID)) {
                    CourseEnrollment patch = new CourseEnrollment();
                    patch.setId(enrollment.getId());
                    patch.setStatus(BizStatusConstant.EnrollmentStatus.REFUNDED);
                    patch.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);
                    courseEnrollmentMapper.updateById(patch);
                }
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

            CourseEnrollment patch = new CourseEnrollment();
            patch.setId(enrollment.getId());
            patch.setStatus(BizStatusConstant.EnrollmentStatus.REFUNDED);
            patch.setAttendStatus(BizStatusConstant.AttendStatus.INVALID);
            courseEnrollmentMapper.updateById(patch);
            success = true;
            return true;
        } finally {
            long costMs = System.currentTimeMillis() - start;
            log.info("COURSE_REFUND userId={} enrollmentId={} success={} costMs={}",
                    userId, enrollmentId, success, costMs);
        }
    }

    public List<CourseEnrollmentHistoryVO> myEnrollments(Long userId) {
        List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getUserId, userId)
                        .orderByDesc(CourseEnrollment::getId)
        );
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }

        Set<Long> courseIds = enrollments.stream()
                .map(CourseEnrollment::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            return enrollments.stream()
                    .map(this::toEnrollmentHistoryVO)
                    .toList();
        }

        Map<Long, String> courseTitleMap = courseMapper.selectList(
                        new LambdaQueryWrapper<Course>()
                                .in(Course::getId, courseIds))
                .stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));
        return enrollments.stream()
                .map(item -> toEnrollmentHistoryVO(item, courseTitleMap.get(item.getCourseId())))
                .toList();
    }

    private CourseEnrollmentHistoryVO toEnrollmentHistoryVO(CourseEnrollment enrollment) {
        return toEnrollmentHistoryVO(enrollment, null);
    }

    private CourseEnrollmentHistoryVO toEnrollmentHistoryVO(CourseEnrollment enrollment, String courseTitle) {
        CourseEnrollmentHistoryVO vo = new CourseEnrollmentHistoryVO();
        vo.setId(enrollment.getId());
        vo.setCourseId(enrollment.getCourseId());
        vo.setCourseTitle(courseTitle);
        vo.setScheduleId(enrollment.getScheduleId());
        vo.setOrderId(enrollment.getOrderId());
        vo.setStatus(enrollment.getStatus());
        vo.setAttendStatus(enrollment.getAttendStatus());
        vo.setEnrollTime(enrollment.getEnrollTime());
        vo.setCreateTime(enrollment.getCreateTime());
        return vo;
    }

    public List<CourseMyScheduleVO> mySchedules(Long userId) {
        List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getUserId, userId)
                        .eq(CourseEnrollment::getStatus, BizStatusConstant.EnrollmentStatus.PAID)
                        .eq(CourseEnrollment::getAttendStatus, BizStatusConstant.AttendStatus.WAIT_CLASS)
                        .orderByDesc(CourseEnrollment::getId)
        );
        if (enrollments == null || enrollments.isEmpty()) {
            return List.of();
        }

        Set<Long> courseIds = enrollments.stream()
                .map(CourseEnrollment::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> scheduleIds = enrollments.stream()
                .map(CourseEnrollment::getScheduleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Course> courseMap = courseIds.isEmpty()
                ? Map.of()
                : courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds))
                .stream()
                .collect(Collectors.toMap(Course::getId, item -> item));

        Map<Long, CourseSchedule> scheduleMap = scheduleIds.isEmpty()
                ? Map.of()
                : courseScheduleMapper.selectList(new LambdaQueryWrapper<CourseSchedule>().in(CourseSchedule::getId, scheduleIds))
                .stream()
                .collect(Collectors.toMap(CourseSchedule::getId, item -> item));

        List<CourseMyScheduleVO> schedules = new ArrayList<>();
        for (CourseEnrollment enrollment : enrollments) {
            Course course = courseMap.get(enrollment.getCourseId());
            CourseSchedule schedule = scheduleMap.get(enrollment.getScheduleId());
            if (course == null || schedule == null) {
                continue;
            }

            CourseMyScheduleVO vo = new CourseMyScheduleVO();
            vo.setEnrollmentId(enrollment.getId());
            vo.setOrderId(enrollment.getOrderId());
            vo.setCourseId(course.getId());
            vo.setCourseTitle(course.getTitle());
            vo.setCoverUrl(course.getCoverUrl());
            vo.setScheduleId(schedule.getId());
            vo.setCoachId(schedule.getCoachId());
            vo.setStartTime(schedule.getStartTime());
            vo.setEndTime(schedule.getEndTime());
            vo.setPrice(course.getPrice());
            vo.setCheckInCode(enrollment.getCheckInCode());
            vo.setAttendStatus(enrollment.getAttendStatus());
            schedules.add(vo);
        }

        schedules.sort(Comparator.comparing(
                CourseMyScheduleVO::getStartTime,
                Comparator.nullsLast(LocalDateTime::compareTo)
        ));
        return schedules;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean checkInByCode(Long userId, String rawCheckInCode) {
        String checkInCode = normalizeCheckInCode(rawCheckInCode);
        if (!StringUtils.hasText(checkInCode)) {
            return false;
        }

        CourseEnrollment enrollment = courseEnrollmentMapper.selectOne(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getCheckInCode, checkInCode)
                        .last("LIMIT 1")
        );
        if (enrollment == null || !Objects.equals(enrollment.getUserId(), userId)) {
            return false;
        }
        return performCheckIn(enrollment);
    }

    private boolean performCheckIn(CourseEnrollment enrollment) {
        if (!Objects.equals(enrollment.getStatus(), BizStatusConstant.EnrollmentStatus.PAID)) {
            return false;
        }
        if (Objects.equals(enrollment.getAttendStatus(), BizStatusConstant.AttendStatus.CHECKED_IN)) {
            return true;
        }
        if (!Objects.equals(enrollment.getAttendStatus(), BizStatusConstant.AttendStatus.WAIT_CLASS)) {
            return false;
        }

        CourseEnrollment patch = new CourseEnrollment();
        patch.setId(enrollment.getId());
        patch.setAttendStatus(BizStatusConstant.AttendStatus.CHECKED_IN);
        return courseEnrollmentMapper.updateById(patch) > 0;
    }

    private void ensureCheckInReady(CourseEnrollment enrollment) {
        if (enrollment == null || !Objects.equals(enrollment.getStatus(), BizStatusConstant.EnrollmentStatus.PAID)) {
            return;
        }

        boolean shouldSetWaitClass = enrollment.getAttendStatus() == null
                || Objects.equals(enrollment.getAttendStatus(), BizStatusConstant.AttendStatus.WAIT_CLASS);
        if (Objects.equals(enrollment.getAttendStatus(), BizStatusConstant.AttendStatus.INVALID)) {
            shouldSetWaitClass = true;
        }
        boolean missingCode = !StringUtils.hasText(enrollment.getCheckInCode());
        if (!shouldSetWaitClass && !missingCode) {
            return;
        }

        for (int attempt = 1; attempt <= CHECK_IN_CODE_MAX_RETRY; attempt++) {
            String candidateCode = missingCode
                    ? CodeUtil.getRandomAlphaNumericCode(CHECK_IN_CODE_LENGTH)
                    : normalizeCheckInCode(enrollment.getCheckInCode());

            CourseEnrollment patch = new CourseEnrollment();
            patch.setId(enrollment.getId());
            if (shouldSetWaitClass) {
                patch.setAttendStatus(BizStatusConstant.AttendStatus.WAIT_CLASS);
            }
            if (missingCode) {
                patch.setCheckInCode(candidateCode);
            }

            try {
                courseEnrollmentMapper.updateById(patch);
                if (shouldSetWaitClass) {
                    enrollment.setAttendStatus(BizStatusConstant.AttendStatus.WAIT_CLASS);
                }
                if (missingCode) {
                    enrollment.setCheckInCode(candidateCode);
                }
                return;
            } catch (DataIntegrityViolationException ex) {
                if (!missingCode || attempt == CHECK_IN_CODE_MAX_RETRY) {
                    throw new HuangException(ResultCodeEnum.SERVICE_ERROR.getCode(), CHECK_IN_CODE_GENERATE_FAILED_MESSAGE);
                }
            }
        }
    }

    private String normalizeCheckInCode(String rawCheckInCode) {
        if (!StringUtils.hasText(rawCheckInCode)) {
            return null;
        }
        return rawCheckInCode.trim().toUpperCase(Locale.ROOT);
    }

    private String genNo(String prefix) {
        return prefix + LocalDateTime.now().toString().replace("-", "").replace(":", "").replace("T", "").replace(".", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }

    private void markCurrentTransactionRollbackOnly() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        }
    }
}
