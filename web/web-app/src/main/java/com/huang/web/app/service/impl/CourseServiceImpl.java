package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.Course;
import com.huang.web.app.mapper.CourseMapper;
import com.huang.web.app.service.CourseService;
import org.springframework.stereotype.Service;

/**
 * Course服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {

}
