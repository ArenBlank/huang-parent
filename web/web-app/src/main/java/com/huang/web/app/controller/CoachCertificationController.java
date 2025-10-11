package com.huang.web.app.controller;

import com.huang.common.result.Result;
import com.huang.web.app.dto.coach.CoachCertificationApplyDTO;
import com.huang.web.app.service.CoachCertificationApplyService;
import com.huang.web.app.vo.coach.CoachCertificationApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * App端教练认证申请控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "教练认证申请", description = "用户端教练认证申请相关接口")
@Slf4j
@RestController
@RequestMapping("/app/coach/certification")
@Validated
public class CoachCertificationController {

    @Autowired
    private CoachCertificationApplyService coachCertificationApplyService;

    @Operation(summary = "提交教练认证申请", description = "用户提交教练认证申请")
    @PostMapping("/apply")
    public Result<CoachCertificationApplyVO> submitApplication(@Valid @RequestBody CoachCertificationApplyDTO dto) {
        try {
            CoachCertificationApplyVO result = coachCertificationApplyService.submitApplication(dto);
            return Result.ok(result);
        } catch (RuntimeException e) {
            log.error("提交教练认证申请失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("提交教练认证申请失败", e);
            return Result.fail("提交申请失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取当前申请状态", description = "获取当前用户最新的教练认证申请状态")
    @GetMapping("/current")
    public Result<CoachCertificationApplyVO> getCurrentApplication() {
        try {
            CoachCertificationApplyVO result = coachCertificationApplyService.getCurrentUserApplication();
            if (result == null) {
                return Result.success("暂无申请记录");
            }
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取当前申请状态失败", e);
            return Result.fail("获取申请状态失败：" + e.getMessage());
        }
    }

    @Operation(summary = "获取申请历史记录", description = "获取当前用户所有的教练认证申请历史记录")
    @GetMapping("/history")
    public Result<List<CoachCertificationApplyVO>> getApplicationHistory() {
        try {
            List<CoachCertificationApplyVO> result = coachCertificationApplyService.getUserApplicationHistory();
            return Result.ok(result);
        } catch (Exception e) {
            log.error("获取申请历史记录失败", e);
            return Result.fail("获取申请历史失败：" + e.getMessage());
        }
    }

    @Operation(summary = "取消申请", description = "取消待审核状态的教练认证申请")
    @DeleteMapping("/{applicationId}/cancel")
    public Result<String> cancelApplication(
            @Parameter(description = "申请ID", required = true)
            @PathVariable @NotNull Long applicationId,
            @Parameter(description = "取消原因")
            @RequestParam(required = false) String cancelReason) {
        try {
            boolean success = coachCertificationApplyService.cancelApplication(applicationId, cancelReason);
            if (success) {
                return Result.ok("申请取消成功");
            } else {
                return Result.fail("申请取消失败");
            }
        } catch (RuntimeException e) {
            log.error("取消申请失败: {}", e.getMessage());
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            log.error("取消申请失败", e);
            return Result.fail("取消申请失败：" + e.getMessage());
        }
    }

    @Operation(summary = "检查申请资格", description = "检查当前用户是否可以申请教练认证")
    @GetMapping("/check-eligibility")
    public Result<Object> checkEligibility() {
        try {
            CoachCertificationApplyVO currentApplication = coachCertificationApplyService.getCurrentUserApplication();
            
            // 构建资格检查结果
            var eligibilityResult = new java.util.HashMap<String, Object>();
            
            if (currentApplication == null) {
                eligibilityResult.put("canApply", true);
                eligibilityResult.put("reason", "可以提交申请");
            } else {
                String status = currentApplication.getStatus();
                switch (status) {
                    case "pending":
                        eligibilityResult.put("canApply", false);
                        eligibilityResult.put("reason", "已有待审核的申请，请等待审核结果");
                        eligibilityResult.put("currentApplicationId", currentApplication.getId());
                        break;
                    case "approved":
                        eligibilityResult.put("canApply", false);
                        eligibilityResult.put("reason", "您已通过教练认证");
                        eligibilityResult.put("certificationNo", currentApplication.getCertificationNo());
                        break;
                    case "rejected":
                    case "cancelled":
                        eligibilityResult.put("canApply", true);
                        eligibilityResult.put("reason", "可以重新申请");
                        eligibilityResult.put("lastApplicationResult", currentApplication.getStatusDesc());
                        eligibilityResult.put("lastRejectReason", currentApplication.getReviewRemark());
                        break;
                    default:
                        eligibilityResult.put("canApply", true);
                        eligibilityResult.put("reason", "可以提交申请");
                }
            }
            
            return Result.ok(eligibilityResult);
        } catch (Exception e) {
            log.error("检查申请资格失败", e);
            return Result.fail("检查申请资格失败：" + e.getMessage());
        }
    }
}