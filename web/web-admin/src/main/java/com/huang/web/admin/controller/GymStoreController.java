package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.util.MinioUtil;
import com.huang.web.admin.dto.store.GymStoreAddDTO;
import com.huang.web.admin.dto.store.GymStoreQueryDTO;
import com.huang.web.admin.dto.store.GymStoreUpdateDTO;
import com.huang.web.admin.service.GymStoreService;
import com.huang.web.admin.vo.store.GymStoreDetailVO;
import com.huang.web.admin.vo.store.GymStoreListVO;
import com.huang.web.admin.vo.store.GymStoreStatisticsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * Admin端门店管理Controller
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "门店管理", description = "门店管理相关接口")
@RestController
@RequestMapping("/admin/gym-store")
@RequiredArgsConstructor
@Slf4j
public class GymStoreController {

    private final GymStoreService gymStoreService;
    private final MinioUtil minioUtil;

    @Operation(summary = "分页查询门店列表", description = "支持按门店名称、编码、地址、状态等条件筛选")
    @GetMapping("/page")
    public Result<Page<GymStoreListVO>> page(@ModelAttribute GymStoreQueryDTO queryDTO) {
        Page<GymStoreListVO> page = gymStoreService.getStoreListPage(queryDTO);
        return Result.success(page);
    }

    @Operation(summary = "查询门店详情", description = "根据门店ID查询详细信息,包含教练、会员、课程统计")
    @GetMapping("/{id}")
    public Result<GymStoreDetailVO> detail(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {
        GymStoreDetailVO detailVO = gymStoreService.getStoreDetail(id);
        return Result.success(detailVO);
    }

    @Operation(summary = "新增门店", description = "创建新的门店信息")
    @PostMapping
    public Result<Long> add(
            @Parameter(description = "门店信息", required = true) @Valid @RequestBody GymStoreAddDTO addDTO) {
        Long storeId = gymStoreService.addStore(addDTO);
        return Result.success(storeId);
    }

    @Operation(summary = "更新门店信息", description = "修改门店的基本信息")
    @PutMapping
    public Result<Void> update(
            @Parameter(description = "门店信息", required = true) @Valid @RequestBody GymStoreUpdateDTO updateDTO) {
        gymStoreService.updateStore(updateDTO);
        return Result.success();
    }

    @Operation(summary = "删除门店", description = "逻辑删除指定门店")
    @DeleteMapping("/{id}")
    public Result<Void> delete(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {
        gymStoreService.deleteStore(id);
        return Result.success();
    }

    @Operation(summary = "批量删除门店", description = "批量逻辑删除多个门店")
    @DeleteMapping("/batch")
    public Result<Void> deleteBatch(
            @Parameter(description = "门店ID列表", required = true) @RequestBody List<Long> ids) {
        gymStoreService.deleteBatch(ids);
        return Result.success();
    }

    @Operation(summary = "修改门店状态", description = "修改门店的营业状态(营业/停业/装修中)")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id,
            @Parameter(description = "状态: 0-停业, 1-营业, 2-装修中", required = true) @RequestParam Integer status) {
        gymStoreService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "查询门店统计数据", description = "获取门店总览统计信息")
    @GetMapping("/statistics")
    public Result<GymStoreStatisticsVO> statistics() {
        GymStoreStatisticsVO statisticsVO = gymStoreService.getStoreStatistics();
        return Result.success(statisticsVO);
    }

    @Operation(summary = "查询门店的教练列表", description = "获取指定门店的所有教练信息")
    @GetMapping("/{id}/coaches")
    public Result<List<Map<String, Object>>> coaches(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {
        List<Map<String, Object>> coaches = gymStoreService.getStoreCoaches(id);
        return Result.success(coaches);
    }

    @Operation(summary = "查询门店的课程列表", description = "获取指定门店的课程排期信息")
    @GetMapping("/{id}/courses")
    public Result<List<Map<String, Object>>> courses(
            @Parameter(description = "门店ID", required = true) @PathVariable Long id) {
        List<Map<String, Object>> courses = gymStoreService.getStoreCourses(id);
        return Result.success(courses);
    }

    @Operation(summary = "上传门店图片", description = "上传门店图片到MinIO,支持jpg、png、jpeg格式,最大10MB")
    @PostMapping("/upload-image")
    public Result<String> uploadImage(
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) {

        // 验证文件
        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的图片");
        }

        // 验证文件类型
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            return Result.fail("只支持上传图片文件");
        }

        // 验证文件大小(10MB)
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.fail("图片大小不能超过10MB");
        }

        try {
            // 上传文件到MinIO
            String fileUrl = minioUtil.uploadFile(file, "store/images");
            log.info("门店图片上传成功: {}", fileUrl);
            return Result.success(fileUrl);
        } catch (Exception e) {
            log.error("门店图片上传失败", e);
            return Result.fail("图片上传失败: " + e.getMessage());
        }
    }
}
