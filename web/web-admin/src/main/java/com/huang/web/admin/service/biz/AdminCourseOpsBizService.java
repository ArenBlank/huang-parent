package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.huang.common.constant.BizStatusConstant;
import com.huang.common.constant.RedisConstant;
import com.huang.common.exception.HuangException;
import com.huang.common.redis.MultiLevelCacheSupport;
import com.huang.common.result.ResultCodeEnum;
import com.huang.common.utils.CodeUtil;
import com.huang.model.entity.Course;
import com.huang.model.entity.CourseEnrollment;
import com.huang.model.entity.CourseSchedule;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.dto.course.CourseScheduleCreateDTO;
import com.huang.web.admin.dto.course.CourseUpsertDTO;
import com.huang.web.admin.mapper.CourseEnrollmentMapper;
import com.huang.web.admin.mapper.CourseMapper;
import com.huang.web.admin.mapper.CourseScheduleMapper;
import com.huang.web.admin.service.core.AdminPermissionScopeService;
import com.huang.web.admin.vo.course.AdminCourseListItemVO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    public List<AdminCourseListItemVO> listCourses(Integer status, Long categoryId) {
        LambdaQueryWrapper<Course> wrapper = new LambdaQueryWrapper<Course>().orderByDesc(Course::getId);
        if (status != null) {
            wrapper.eq(Course::getStatus, status);
        }
        if (categoryId != null) {
            wrapper.eq(Course::getCategoryId, categoryId);
        }

        List<Course> courses = courseMapper.selectList(wrapper);
        if (courses == null || courses.isEmpty()) {
            return List.of();
        }

        Map<Long, CourseLifecycleSnapshot> snapshotMap = buildLifecycleSnapshotMap(courses);
        return courses.stream()
                .map(course -> toListItem(course, snapshotMap.get(course.getId())))
                .sorted(Comparator.comparing(AdminCourseListItemVO::getId, Comparator.nullsLast(Long::compareTo)).reversed())
                .collect(Collectors.toList());
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
    public boolean deleteCourse(Long id) {
        Course exists = courseMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        adminPermissionScopeService.assertCourseCategoryAccess(exists.getCategoryId(), "course:delete", "courseId=" + id);

        Long enrollmentCount = courseEnrollmentMapper.selectCount(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .eq(CourseEnrollment::getCourseId, id)
        );
        if (enrollmentCount != null && enrollmentCount > 0) {
            throw new HuangException(AdminErrorCode.COURSE_DELETE_FORBIDDEN, "课程已有报名记录，暂不支持删除");
        }

        courseScheduleMapper.delete(
                new LambdaQueryWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCourseId, id)
        );
        boolean deleted = courseMapper.deleteById(id) > 0;
        if (deleted) {
            clearCourseListCaches();
        }
        return deleted;
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
    public CourseGovernanceResult governCourseLifecycle() {
        List<Course> courses = courseMapper.selectList(new LambdaQueryWrapper<Course>().orderByDesc(Course::getId));
        if (courses == null || courses.isEmpty()) {
            return CourseGovernanceResult.empty();
        }

        Map<Long, CourseLifecycleSnapshot> snapshotMap = buildLifecycleSnapshotMap(courses);
        int autoUnpublishedCourses = 0;
        int autoDeletedCourses = 0;
        int autoClosedSchedules = 0;
        boolean changed = false;

        for (Course course : courses) {
            CourseLifecycleSnapshot snapshot = snapshotMap.get(course.getId());
            if (snapshot == null || !LifecycleStatus.ENDED.name().equals(snapshot.getLifecycleStatus())) {
                continue;
            }

            if (snapshot.isHasEnrollment()) {
                if (!Objects.equals(course.getStatus(), 0)) {
                    Course patch = new Course();
                    patch.setId(course.getId());
                    patch.setStatus(0);
                    courseMapper.updateById(patch);
                    autoUnpublishedCourses++;
                    changed = true;
                }

                int closedSchedules = closeEndedSchedules(course.getId());
                if (closedSchedules > 0) {
                    autoClosedSchedules += closedSchedules;
                    changed = true;
                }
                continue;
            }

            courseScheduleMapper.delete(
                    new LambdaQueryWrapper<CourseSchedule>()
                            .eq(CourseSchedule::getCourseId, course.getId())
            );
            courseMapper.deleteById(course.getId());
            autoDeletedCourses++;
            changed = true;
        }

        if (changed) {
            clearCourseListCaches();
        }
        return new CourseGovernanceResult(autoUnpublishedCourses, autoDeletedCourses, autoClosedSchedules);
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

    private Map<Long, CourseLifecycleSnapshot> buildLifecycleSnapshotMap(List<Course> courses) {
        if (courses == null || courses.isEmpty()) {
            return Map.of();
        }

        Set<Long> courseIds = courses.stream()
                .map(Course::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (courseIds.isEmpty()) {
            return Map.of();
        }

        List<CourseSchedule> schedules = courseScheduleMapper.selectList(
                new LambdaQueryWrapper<CourseSchedule>()
                        .in(CourseSchedule::getCourseId, courseIds)
        );
        List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(
                new LambdaQueryWrapper<CourseEnrollment>()
                        .in(CourseEnrollment::getCourseId, courseIds)
        );

        Map<Long, List<CourseSchedule>> schedulesByCourseId = schedules == null
                ? Map.of()
                : schedules.stream().collect(Collectors.groupingBy(CourseSchedule::getCourseId));

        Map<Long, Long> enrollmentCountMap = new HashMap<>();
        if (enrollments != null) {
            for (CourseEnrollment enrollment : enrollments) {
                if (enrollment.getCourseId() == null) {
                    continue;
                }
                enrollmentCountMap.merge(enrollment.getCourseId(), 1L, Long::sum);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Map<Long, CourseLifecycleSnapshot> snapshotMap = new HashMap<>();
        for (Course course : courses) {
            List<CourseSchedule> courseSchedules = schedulesByCourseId.getOrDefault(course.getId(), List.of());
            snapshotMap.put(
                    course.getId(),
                    buildLifecycleSnapshot(courseSchedules, enrollmentCountMap.getOrDefault(course.getId(), 0L) > 0, now)
            );
        }
        return snapshotMap;
    }

    private CourseLifecycleSnapshot buildLifecycleSnapshot(List<CourseSchedule> schedules,
                                                          boolean hasEnrollment,
                                                          LocalDateTime now) {
        CourseLifecycleSnapshot snapshot = new CourseLifecycleSnapshot();
        snapshot.setHasEnrollment(hasEnrollment);
        snapshot.setTotalSchedules(schedules == null ? 0 : schedules.size());

        if (schedules == null || schedules.isEmpty()) {
            snapshot.setLifecycleStatus(LifecycleStatus.NO_SCHEDULE.name());
            snapshot.setLifecycleLabel(LifecycleStatus.NO_SCHEDULE.getLabel());
            snapshot.setActiveSchedules(0);
            return snapshot;
        }

        int activeSchedules = 0;
        boolean hasOngoing = false;
        boolean hasFuture = false;
        LocalDateTime latestStartTime = null;
        LocalDateTime latestEndTime = null;

        for (CourseSchedule schedule : schedules) {
            if (schedule == null) {
                continue;
            }

            LocalDateTime startTime = schedule.getStartTime();
            LocalDateTime endTime = schedule.getEndTime();
            if (latestStartTime == null || (startTime != null && startTime.isAfter(latestStartTime))) {
                latestStartTime = startTime;
            }
            if (latestEndTime == null || (endTime != null && endTime.isAfter(latestEndTime))) {
                latestEndTime = endTime;
            }

            boolean notEnded = endTime == null || !endTime.isBefore(now);
            if (Objects.equals(schedule.getStatus(), 1) && notEnded) {
                activeSchedules++;
            }
            if (startTime != null && endTime != null && !startTime.isAfter(now) && !endTime.isBefore(now)) {
                hasOngoing = true;
            }
            if (startTime != null && startTime.isAfter(now)) {
                hasFuture = true;
            } else if (startTime == null && notEnded) {
                hasFuture = true;
            }
        }

        LifecycleStatus lifecycleStatus;
        if (hasOngoing) {
            lifecycleStatus = LifecycleStatus.ONGOING;
        } else if (hasFuture) {
            lifecycleStatus = LifecycleStatus.UPCOMING;
        } else {
            lifecycleStatus = LifecycleStatus.ENDED;
        }

        snapshot.setLifecycleStatus(lifecycleStatus.name());
        snapshot.setLifecycleLabel(lifecycleStatus.getLabel());
        snapshot.setActiveSchedules(activeSchedules);
        snapshot.setLatestStartTime(latestStartTime);
        snapshot.setLatestEndTime(latestEndTime);
        return snapshot;
    }

    private AdminCourseListItemVO toListItem(Course course, CourseLifecycleSnapshot snapshot) {
        AdminCourseListItemVO vo = new AdminCourseListItemVO();
        vo.setId(course.getId());
        vo.setCategoryId(course.getCategoryId());
        vo.setTitle(course.getTitle());
        vo.setSummary(course.getSummary());
        vo.setCoverUrl(course.getCoverUrl());
        vo.setLevel(course.getLevel());
        vo.setDurationMin(course.getDurationMin());
        vo.setPrice(course.getPrice());
        vo.setStatus(course.getStatus());
        if (snapshot != null) {
            vo.setLifecycleStatus(snapshot.getLifecycleStatus());
            vo.setLifecycleLabel(snapshot.getLifecycleLabel());
            vo.setTotalSchedules(snapshot.getTotalSchedules());
            vo.setActiveSchedules(snapshot.getActiveSchedules());
            vo.setHasEnrollment(snapshot.isHasEnrollment());
            vo.setLatestStartTime(snapshot.getLatestStartTime());
            vo.setLatestEndTime(snapshot.getLatestEndTime());
        } else {
            vo.setLifecycleStatus(LifecycleStatus.NO_SCHEDULE.name());
            vo.setLifecycleLabel(LifecycleStatus.NO_SCHEDULE.getLabel());
            vo.setTotalSchedules(0);
            vo.setActiveSchedules(0);
            vo.setHasEnrollment(false);
        }
        return vo;
    }

    private int closeEndedSchedules(Long courseId) {
        return courseScheduleMapper.update(
                null,
                new LambdaUpdateWrapper<CourseSchedule>()
                        .eq(CourseSchedule::getCourseId, courseId)
                        .eq(CourseSchedule::getStatus, 1)
                        .lt(CourseSchedule::getEndTime, LocalDateTime.now())
                        .set(CourseSchedule::getStatus, 0)
        );
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

    private enum LifecycleStatus {
        UPCOMING("未开始"),
        ONGOING("进行中"),
        ENDED("已结束"),
        NO_SCHEDULE("无排期");

        private final String label;

        LifecycleStatus(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private static class CourseLifecycleSnapshot {
        private String lifecycleStatus;
        private String lifecycleLabel;
        private int totalSchedules;
        private int activeSchedules;
        private boolean hasEnrollment;
        private LocalDateTime latestStartTime;
        private LocalDateTime latestEndTime;

        public String getLifecycleStatus() {
            return lifecycleStatus;
        }

        public void setLifecycleStatus(String lifecycleStatus) {
            this.lifecycleStatus = lifecycleStatus;
        }

        public String getLifecycleLabel() {
            return lifecycleLabel;
        }

        public void setLifecycleLabel(String lifecycleLabel) {
            this.lifecycleLabel = lifecycleLabel;
        }

        public int getTotalSchedules() {
            return totalSchedules;
        }

        public void setTotalSchedules(int totalSchedules) {
            this.totalSchedules = totalSchedules;
        }

        public int getActiveSchedules() {
            return activeSchedules;
        }

        public void setActiveSchedules(int activeSchedules) {
            this.activeSchedules = activeSchedules;
        }

        public boolean isHasEnrollment() {
            return hasEnrollment;
        }

        public void setHasEnrollment(boolean hasEnrollment) {
            this.hasEnrollment = hasEnrollment;
        }

        public LocalDateTime getLatestStartTime() {
            return latestStartTime;
        }

        public void setLatestStartTime(LocalDateTime latestStartTime) {
            this.latestStartTime = latestStartTime;
        }

        public LocalDateTime getLatestEndTime() {
            return latestEndTime;
        }

        public void setLatestEndTime(LocalDateTime latestEndTime) {
            this.latestEndTime = latestEndTime;
        }
    }

    public static class CourseGovernanceResult {
        private final int autoUnpublishedCourses;
        private final int autoDeletedCourses;
        private final int autoClosedSchedules;

        public CourseGovernanceResult(int autoUnpublishedCourses, int autoDeletedCourses, int autoClosedSchedules) {
            this.autoUnpublishedCourses = autoUnpublishedCourses;
            this.autoDeletedCourses = autoDeletedCourses;
            this.autoClosedSchedules = autoClosedSchedules;
        }

        public static CourseGovernanceResult empty() {
            return new CourseGovernanceResult(0, 0, 0);
        }

        public int getAutoUnpublishedCourses() {
            return autoUnpublishedCourses;
        }

        public int getAutoDeletedCourses() {
            return autoDeletedCourses;
        }

        public int getAutoClosedSchedules() {
            return autoClosedSchedules;
        }
    }
}
