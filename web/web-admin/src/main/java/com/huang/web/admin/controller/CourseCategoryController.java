package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.result.Result;
import com.huang.model.entity.CourseCategory;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.service.CourseCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Admin课程分类管理", description = "课程分类查询")
@RestController
@RequestMapping("/admin/course-category")
@RequireAdminRole(AdminRoleCode.ADMIN)
public class CourseCategoryController {

    private final CourseCategoryService courseCategoryService;

    public CourseCategoryController(CourseCategoryService courseCategoryService) {
        this.courseCategoryService = courseCategoryService;
    }

    @Operation(summary = "课程分类列表")
    @RequireAdminPermission({"course:read"})
    @GetMapping("/list")
    public Result<List<CourseCategory>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<CourseCategory> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(CourseCategory::getStatus, status);
        }
        wrapper.orderByAsc(CourseCategory::getSort).orderByAsc(CourseCategory::getId);
        return Result.ok(courseCategoryService.list(wrapper));
    }
}
