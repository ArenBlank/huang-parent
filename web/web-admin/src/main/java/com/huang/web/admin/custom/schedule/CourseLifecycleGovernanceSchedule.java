package com.huang.web.admin.custom.schedule;

import com.huang.common.constant.TaskRunConstant;
import com.huang.common.guard.DistributedTaskLock;
import com.huang.web.admin.service.biz.AdminCourseOpsBizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CourseLifecycleGovernanceSchedule {

    private final AdminCourseOpsBizService adminCourseOpsBizService;

    public CourseLifecycleGovernanceSchedule(AdminCourseOpsBizService adminCourseOpsBizService) {
        this.adminCourseOpsBizService = adminCourseOpsBizService;
    }

    @Scheduled(fixedDelay = 300000)
    @DistributedTaskLock(taskCode = TaskRunConstant.TASK_COURSE_LIFECYCLE_GOVERN, ttlSec = 180)
    public void governCourseLifecycle() {
        AdminCourseOpsBizService.CourseGovernanceResult result = adminCourseOpsBizService.governCourseLifecycle();
        if (result.getAutoUnpublishedCourses() > 0
                || result.getAutoDeletedCourses() > 0
                || result.getAutoClosedSchedules() > 0) {
            log.info(
                    "course lifecycle governance finished, unpublishedCourses={}, deletedCourses={}, closedSchedules={}",
                    result.getAutoUnpublishedCourses(),
                    result.getAutoDeletedCourses(),
                    result.getAutoClosedSchedules()
            );
        }
    }
}
