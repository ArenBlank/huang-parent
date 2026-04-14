package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.exception.HuangException;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.CodeUtil;
import com.huang.model.entity.Course;
import com.huang.model.entity.CourseEnrollment;
import com.huang.model.entity.CourseSchedule;
import com.huang.web.admin.dto.course.CourseScheduleCreateDTO;
import com.huang.web.admin.dto.course.CourseUpsertDTO;
import com.huang.web.admin.mapper.CourseEnrollmentMapper;
import com.huang.web.admin.mapper.CourseMapper;
import com.huang.web.admin.mapper.CourseScheduleMapper;
import com.huang.web.admin.service.core.AdminPermissionScopeService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class AdminCourseOpsBizService {

    private static final int CHECK_IN_CODE_LENGTH = 6;
    private static final int CHECK_IN_CODE_MAX_RETRY = 10;
    private static final String CHECK_IN_CODE_GENERATE_FAILED_MESSAGE = "核销码生成失败，请稍后再试";

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final AdminPermissionScopeService adminPermissionScopeService;
    private final MultiLevelCacheSupport multiLevelCacheSupport;

    public AdminCourseOpsBizService(CourseMapper courseMapper,
                                    CourseScheduleMapper courseScheduleMapper,
                                    CourseEnrollmentMapper courseEnrollmentMapper,
                                    AdminPermissionScopeService adminPermissionScopeService,
                                    MultiLevelCacheSupport multiLevelCacheSupport) {
        this.courseMapper = courseMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.courseEnrollmentMapper = courseEnrollmentMapper;
        this.adminPermissionScopeService = adminPermissionScopeService;
        this.multiLevelCacheSupport = multiLevelCacheSupport;
    }

    public List<Course> listCourses(Integer status, Long categoryId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>().orderByDesc(Course::getId);
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Course::getCategoryId, categoryId);
        }
        return courseMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createCourse(CourseUpsertDTO dto) {
        adminPermissionScopeService.assertCourseCategoryAccess(dto.getCategoryId(), "course:create", null);
        Course course = new Course();
        fillCourse(course, dto);
        courseMapper.insert(course);
        clearCourseListCaches();
        return course.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourse(Long id, CourseUpsertDTO dto) {
        Course exists = courseMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        Long categoryId = dto.getCategoryId() == null ? exists.getCategoryId() : dto.getCategoryId();
        adminPermissionScopeService.assertCourseCategoryAccess(categoryId, "course:update", "courseId=" + id);
        fillCourse(exists, dto);
        boolean updated = courseMapper.updateById(exists) > 0;
        if (updated) {
            clearCourseListCaches();
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseStatus(Long id, Integer status) {
        Course exists = courseMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        adminPermissionScopeService.assertCourseCategoryAccess(exists.getCategoryId(), "course:publish", "courseId=" + id);
        exists.setStatus(status);
        boolean updated = courseMapper.updateById(exists) > 0;
        if (updated) {
            clearCourseListCaches();
        }
        return updated;
    }

    public List<CourseSchedule> listSchedules(Long courseId, Integer status) {
        LambdaQueryWrapper<CourseSchedule> wrapper = new LambdaQueryWrapper<CourseSchedule>()
                .orderByAsc(CourseSchedule::getStartTime);
        if (courseId != null) {
            wrapper.eq(CourseSchedule::getCourseId, courseId);
        }
        if (status != null) {
            wrapper.eq(CourseSchedule::getStatus, status);
        }
        return courseScheduleMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(CourseScheduleCreateDTO dto) {
        Course course = courseMapper.selectById(dto.getCourseId());
        if (course == null) {
            return null;
        }
        adminPermissionScopeService.assertCourseCategoryAccess(course.getCategoryId(), "course:schedule", "courseId=" + course.getId());
        CourseSchedule schedule = new CourseSchedule();
        schedule.setCourseId(dto.getCourseId());
        schedule.setCoachId(dto.getCoachId());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setCapacity(dto.getCapacity());
        schedule.setBookedCount(0);
        schedule.setStatus(dto.getStatus());
        courseScheduleMapper.insert(schedule);
        clearCourseListCaches();
        return schedule.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateScheduleStatus(Long id, Integer status) {
        CourseSchedule schedule = courseScheduleMapper.selectById(id);
        if (schedule == null) {
            return false;
        }
        Course course = courseMapper.selectById(schedule.getCourseId());
        if (course != null) {
            adminPermissionScopeService.assertCourseCategoryAccess(course.getCategoryId(), "course:schedule", "scheduleId=" + id);
        }
        schedule.setStatus(status);
        boolean updated = courseScheduleMapper.updateById(schedule) > 0;
        if (updated) {
            clearCourseListCaches();
        }
        return updated;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean checkInByCode(String rawCheckInCode) {
        String checkInCode = normalizeCheckInCode(rawCheckInCode);
        if (!StringUtils.hasText(checkInCode)) {
            return false;
        }
        CourseEnrollment enrollment = courseEnrollmentMapper.selectOne(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getCheckInCode, checkInCode)
                        .last("LIMIT 1")
        );
        if (enrollment == null) {
            return false;
        }
        return performCheckIn(enrollment);
    }

    @Transactional(rollbackFor = Exception.class)
    public int fixHistoryCheckInCodes() {
        List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getStatus, BizStatusConstant.EnrollmentStatus.PAID)
                        .isNull(CourseEnrollment::getCheckInCode)
                        .orderByAsc(CourseEnrollment::getId)
        );
        if (enrollments == null || enrollments.isEmpty()) {
            return 0;
        }

        int fixed = 0;
        for (CourseEnrollment enrollment : enrollments) {
            ensureCheckInReady(enrollment);
            fixed++;
        }
        return fixed;
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

    private void fillCourse(Course course, CourseUpsertDTO dto) {
        course.setCategoryId(dto.getCategoryId());
        course.setTitle(dto.getTitle());
        course.setSummary(dto.getSummary());
        course.setCoverUrl(dto.getCoverUrl());
        course.setLevel(dto.getLevel());
        course.setDurationMin(dto.getDurationMin());
        course.setPrice(dto.getPrice());
        course.setStatus(dto.getStatus());
    }

    private void clearCourseListCaches() {
        multiLevelCacheSupport.sharedEvictByPrefix(RedisConstant.APP_COURSE_LIST_PREFIX);
    }
}
