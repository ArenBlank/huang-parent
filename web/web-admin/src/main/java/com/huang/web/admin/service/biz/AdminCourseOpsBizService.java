package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.Course;
import com.huang.model.entity.CourseSchedule;
import com.huang.web.admin.dto.course.CourseScheduleCreateDTO;
import com.huang.web.admin.dto.course.CourseUpsertDTO;
import com.huang.web.admin.mapper.CourseMapper;
import com.huang.web.admin.mapper.CourseScheduleMapper;
import com.huang.web.admin.service.core.AdminPermissionScopeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminCourseOpsBizService {

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final AdminPermissionScopeService adminPermissionScopeService;

    public AdminCourseOpsBizService(CourseMapper courseMapper,
                                    CourseScheduleMapper courseScheduleMapper,
                                    AdminPermissionScopeService adminPermissionScopeService) {
        this.courseMapper = courseMapper;
        this.courseScheduleMapper = courseScheduleMapper;
        this.adminPermissionScopeService = adminPermissionScopeService;
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
        return courseMapper.updateById(exists) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateCourseStatus(Long id, Integer status) {
        Course exists = courseMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        adminPermissionScopeService.assertCourseCategoryAccess(exists.getCategoryId(), "course:publish", "courseId=" + id);
        exists.setStatus(status);
        return courseMapper.updateById(exists) > 0;
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
        return courseScheduleMapper.updateById(schedule) > 0;
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
}
