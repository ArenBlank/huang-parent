package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.CourseEnrollment;
import com.huang.web.app.mapper.CourseEnrollmentMapper;
import com.huang.web.app.service.CourseEnrollmentService;
import org.springframework.stereotype.Service;

/**
 * CourseEnrollment服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class CourseEnrollmentServiceImpl extends ServiceImpl<CourseEnrollmentMapper, CourseEnrollment> implements CourseEnrollmentService {

}
