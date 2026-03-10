package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.CourseCategory;
import com.huang.web.admin.mapper.CourseCategoryMapper;
import com.huang.web.admin.service.CourseCategoryService;
import org.springframework.stereotype.Service;

@Service
public class CourseCategoryServiceImpl extends ServiceImpl<CourseCategoryMapper, CourseCategory> implements CourseCategoryService {
}
