package com.huang.web.admin.controller;

import com.huang.common.result.Result;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.constant.AdminRoleCode;
import com.huang.web.admin.custom.annotation.RequireAdminRole;
import com.huang.web.admin.custom.aop.OperationLog;
import com.huang.web.admin.dto.plan.TrainingPlanItemUpsertDTO;
import com.huang.web.admin.dto.plan.TrainingPlanUpsertDTO;
import com.huang.web.admin.service.biz.AdminTrainingPlanBizService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Training Plan", description = "Training plan and plan item management")
@RestController
@RequestMapping("/admin/training-plan")
@RequireAdminRole({AdminRoleCode.ADMIN, AdminRoleCode.OPS_ADMIN})
public class TrainingPlanManageController {

    private final AdminTrainingPlanBizService adminTrainingPlanBizService;

    public TrainingPlanManageController(AdminTrainingPlanBizService adminTrainingPlanBizService) {
        this.adminTrainingPlanBizService = adminTrainingPlanBizService;
    }

    @Operation(summary = "List training plans")
    @GetMapping("/list")
    public Result<?> list(@RequestParam(required = false) Integer status) {
        return Result.ok(adminTrainingPlanBizService.listPlans(status));
    }

    @Operation(summary = "Get training plan detail")
    @GetMapping("/{planId}")
    public Result<?> detail(@PathVariable Long planId) {
        var detail = adminTrainingPlanBizService.getPlanDetail(planId);
        return detail == null
                ? Result.fail(AdminErrorCode.TRAINING_PLAN_NOT_FOUND, "训练计划不存在")
                : Result.ok(detail);
    }

    @Operation(summary = "Create training plan")
    @PostMapping
    @OperationLog(module = "training_plan", action = "create", detail = "admin create training plan")
    public Result<?> create(@Valid @RequestBody TrainingPlanUpsertDTO dto) {
        return Result.ok(adminTrainingPlanBizService.createPlan(dto));
    }

    @Operation(summary = "Update training plan")
    @PutMapping("/{id}")
    @OperationLog(module = "training_plan", action = "update", detail = "admin update training plan")
    public Result<?> update(@PathVariable Long id, @Valid @RequestBody TrainingPlanUpsertDTO dto) {
        boolean ok = adminTrainingPlanBizService.updatePlan(id, dto);
        return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.TRAINING_PLAN_NOT_FOUND, "训练计划不存在");
    }

    @Operation(summary = "Delete training plan")
    @DeleteMapping("/{id}")
    @OperationLog(module = "training_plan", action = "delete", detail = "admin delete training plan")
    public Result<?> delete(@PathVariable Long id) {
        try {
            boolean ok = adminTrainingPlanBizService.deletePlan(id);
            return ok ? Result.ok("删除成功") : Result.fail(AdminErrorCode.TRAINING_PLAN_NOT_FOUND, "训练计划不存在");
        } catch (IllegalStateException e) {
            return Result.fail(AdminErrorCode.TRAINING_PLAN_DELETE_FAILED, e.getMessage());
        }
    }

    @Operation(summary = "Create training plan item")
    @PostMapping("/{planId}/item")
    @OperationLog(module = "training_plan_item", action = "create", detail = "admin create training plan item")
    public Result<?> createItem(@PathVariable Long planId, @Valid @RequestBody TrainingPlanItemUpsertDTO dto) {
        try {
            Long id = adminTrainingPlanBizService.createPlanItem(planId, dto);
            return id == null
                    ? Result.fail(AdminErrorCode.TRAINING_PLAN_NOT_FOUND, "训练计划不存在")
                    : Result.ok(id);
        } catch (IllegalStateException e) {
            return Result.fail(AdminErrorCode.VIDEO_BIND_FAILED, e.getMessage());
        }
    }

    @Operation(summary = "Update training plan item")
    @PutMapping("/item/{itemId}")
    @OperationLog(module = "training_plan_item", action = "update", detail = "admin update training plan item")
    public Result<?> updateItem(@PathVariable Long itemId, @Valid @RequestBody TrainingPlanItemUpsertDTO dto) {
        try {
            boolean ok = adminTrainingPlanBizService.updatePlanItem(itemId, dto);
            return ok ? Result.ok("更新成功") : Result.fail(AdminErrorCode.TRAINING_PLAN_ITEM_NOT_FOUND, "计划项不存在");
        } catch (IllegalStateException e) {
            return Result.fail(AdminErrorCode.VIDEO_BIND_FAILED, e.getMessage());
        }
    }

    @Operation(summary = "Delete training plan item")
    @DeleteMapping("/item/{itemId}")
    @OperationLog(module = "training_plan_item", action = "delete", detail = "admin delete training plan item")
    public Result<?> deleteItem(@PathVariable Long itemId) {
        try {
            boolean ok = adminTrainingPlanBizService.deletePlanItem(itemId);
            return ok ? Result.ok("删除成功") : Result.fail(AdminErrorCode.TRAINING_PLAN_ITEM_NOT_FOUND, "计划项不存在");
        } catch (IllegalStateException e) {
            return Result.fail(AdminErrorCode.TRAINING_PLAN_DELETE_FAILED, e.getMessage());
        }
    }
}
