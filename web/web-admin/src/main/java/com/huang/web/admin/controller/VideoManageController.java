package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminPermission;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.video.PlanItemVideoBindDTO;
import com.huang.web.admin.dto.video.VideoAssetUpsertDTO;
import com.huang.web.admin.service.biz.VideoContentBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Admin视频内容", description = "教学视频素材管理与计划项绑定")
@RestController
@RequestMapping("/admin/video")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class VideoManageController {

    private final VideoContentBizService videoContentBizService;

    public VideoManageController(VideoContentBizService videoContentBizService) {
        this.videoContentBizService = videoContentBizService;
    }

    @Operation(summary = "视频素材列表")
    @RequireAdminPermission({"video:asset"})
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status,
                          @RequestParam(required = false) String keyword) {
        return Result.ok(videoContentBizService.list(status, keyword));
    }

    @Operation(summary = "新增视频素材")
    @PostMapping
    @RequireAdminPermission({"video:asset"})
    @OperationLog(module = "video", action = "create", detail = "admin create video asset")
    public Result<?> create(@Valid @RequestBody VideoAssetUpsertDTO dto) {
        return Result.ok(videoContentBizService.create(dto));
    }

    @Operation(summary = "上传视频到MinIO")
    @PostMapping("/upload")
    @RequireAdminPermission({"video:upload"})
    @OperationLog(module = "video", action = "upload", detail = "admin upload video")
    public Result<?> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(videoContentBizService.uploadVideo(file));
    }

    @Operation(summary = "更新视频素材")
    @PutMapping("/{id}")
    @RequireAdminPermission({"video:asset"})
    @OperationLog(module = "video", action = "update", detail = "admin update video asset")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody VideoAssetUpsertDTO dto) {
        boolean ok = videoContentBizService.update(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.VIDEO_ASSET_NOT_FOUND, "素材不存在");
    }

    @Operation(summary = "更新视频素材状态")
    @PutMapping("/{id}/status")
    @RequireAdminPermission({"video:status"})
    @OperationLog(module = "video", action = "update_status", detail = "admin update video status")
    public Result<?> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        boolean ok = videoContentBizService.updateStatus(id, status);
        return ok ? Result.ok("状态更新成功") : Result.fail(AdminErrorCode.VIDEO_ASSET_NOT_FOUND, "素材不存在");
    }

    @Operation(summary = "训练计划项绑定视频")
    @PutMapping("/bind-plan-item")
    @RequireAdminPermission({"video:bind"})
    @OperationLog(module = "video", action = "bind_plan_item", detail = "admin bind video to plan item")
    public Result<?> bindPlanItem(@Valid @RequestBody PlanItemVideoBindDTO dto) {
        boolean ok = videoContentBizService.bindToPlanItem(dto.getPlanItemId(), dto.getVideoId());
        return ok ? Result.ok("绑定成功") : Result.fail(AdminErrorCode.VIDEO_BIND_FAILED, "绑定失败：计划项不存在或视频不可用");
    }

    @Operation(summary = "训练计划项解绑视频")
    @PutMapping("/unbind-plan-item/{planItemId}")
    @RequireAdminPermission({"video:bind"})
    @OperationLog(module = "video", action = "unbind_plan_item", detail = "admin unbind video from plan item")
    public Result<?> unbindPlanItem(@PathVariable Long planItemId) {
        boolean ok = videoContentBizService.unbindFromPlanItem(planItemId);
        return ok ? Result.ok("解绑成功") : Result.fail(AdminErrorCode.VIDEO_BIND_FAILED, "计划项不存在");
    }
}
