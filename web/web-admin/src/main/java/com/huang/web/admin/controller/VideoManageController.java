package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.dto.video.PlanItemVideoBindDTO;
import com.huang.web.admin.dto.video.VideoAssetUpsertDTO;
import com.huang.web.admin.service.biz.VideoContentBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin视频内容", description = "教学视频素材管理与训练计划项绑定")
@RestController
@RequestMapping("/admin/video")
public class VideoManageController {

    private final VideoContentBizService videoContentBizService;

    public VideoManageController(VideoContentBizService videoContentBizService) {
        this.videoContentBizService = videoContentBizService;
    }

    @Operation(summary = "视频素材列表")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(videoContentBizService.list(status, keyword));
    }

    @Operation(summary = "新增视频素材")
    @PostMapping
    public Result<?> create(@Valid @RequestBody VideoAssetUpsertDTO dto) {
        return Result.ok(videoContentBizService.create(dto));
    }

    @Operation(summary = "上传视频到MinIO")
    @PostMapping("/upload")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(videoContentBizService.uploadVideo(file));
    }

    @Operation(summary = "更新视频素材")
    @PutMapping("/{id}")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody VideoAssetUpsertDTO dto) {
        boolean ok = videoContentBizService.update(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail("素材不存在");
    }

    @Operation(summary = "更新视频素材状态")
    @PutMapping("/{id}/status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean ok = videoContentBizService.updateStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail("素材不存在");
    }

    @Operation(summary = "训练计划项绑定视频")
    @PutMapping("/bind-plan-item")
    public Result<?> bindPlanItem(@Valid @RequestBody PlanItemVideoBindDTO dto) {
        boolean ok = videoContentBizService.bindToPlanItem(dto.getPlanItemId(), dto.getVideoId());
        return ok ? Result.ok("绑定成功") : Result.fail("绑定失败，计划项或视频不可用");
    }

    @Operation(summary = "训练计划项解绑视频")
    @PutMapping("/unbind-plan-item/{planItemId}")
    public Result<?> unbindPlanItem(@PathVariable Long planItemId) {
        boolean ok = videoContentBizService.unbindFromPlanItem(planItemId);
        return ok ? Result.ok("解绑成功") : Result.fail("计划项不存在");
    }
}
