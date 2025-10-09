package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.result.Result;
import com.huang.common.result.ResultCodeEnum;
import com.huang.web.admin.dto.coach.CoachCertificationQueryDTO;
import com.huang.web.admin.dto.coach.CoachCertificationReviewDTO;
import com.huang.web.admin.service.AdminCoachCertificationService;
import com.huang.web.admin.vo.coach.CoachCertificationDetailVO;
import com.huang.web.admin.vo.coach.CoachCertificationListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Admin端教练认证申请管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "Admin-教练认证管理", description = "管理端教练认证申请审核相关接口")
@RestController
@RequestMapping("/admin/coach/certification")
@Validated
@Slf4j
public class CoachCertificationController {

    @Autowired
    private AdminCoachCertificationService adminCoachCertificationService;

    /**
     * 分页查询教练认证申请列表
     */
    @Operation(summary = "分页查询教练认证申请列表", description = "支持多条件筛选和分页查询教练认证申请")
    @GetMapping("/list")
    public Result<Page<CoachCertificationListVO>> getApplicationList(@Valid CoachCertificationQueryDTO queryDTO) {
        try {
            Page<CoachCertificationListVO> resultPage = adminCoachCertificationService.getApplicationList(queryDTO);
            return Result.ok(resultPage);
        } catch (Exception e) {
            log.error("查询教练认证申请列表失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "查询教练认证申请列表失败：" + e.getMessage());
        }
    }

    /**
     * 获取教练认证申请详情
     */
    @Operation(summary = "获取教练认证申请详情", description = "根据申请ID查看教练认证申请的详细信息")
    @GetMapping("/detail/{id}")
    public Result<CoachCertificationDetailVO> getApplicationDetail(
            @Parameter(description = "申请ID", required = true)
            @PathVariable @NotNull @Min(1) Long id) {
        try {
            CoachCertificationDetailVO detailVO = adminCoachCertificationService.getApplicationDetail(id);
            if (detailVO == null) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "教练认证申请不存在");
            }
            return Result.ok(detailVO);
        } catch (Exception e) {
            log.error("获取教练认证申请详情失败，id: {}", id, e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "获取教练认证申请详情失败：" + e.getMessage());
        }
    }

    /**
     * 审核教练认证申请
     */
    @Operation(summary = "审核教练认证申请", description = "审核通过或拒绝教练认证申请")
    @PutMapping("/review")
    public Result<Void> reviewApplication(@RequestBody @Valid CoachCertificationReviewDTO reviewDTO) {
        try {
            boolean success = adminCoachCertificationService.reviewApplication(reviewDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "审核教练认证申请失败");
            }
        } catch (RuntimeException e) {
            log.error("审核教练认证申请失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核教练认证申请失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "审核教练认证申请失败：" + e.getMessage());
        }
    }

    /**
     * 审核教练认证申请（表单参数版本）
     */
    @Operation(summary = "审核教练认证申请（表单版）", description = "使用表单参数审核教练认证申请，方便接口文档测试")
    @PutMapping("/review-form")
    public Result<Void> reviewApplicationForm(
            @Parameter(description = "申请ID", required = true)
            @RequestParam @NotNull @Min(1) Long id,
            @Parameter(description = "审核结果: approved已通过 rejected已拒绝", required = true)
            @RequestParam @NotNull String status,
            @Parameter(description = "审核备注")
            @RequestParam(required = false) String reviewRemark,
            @Parameter(description = "认证编号（通过时可自定义）")
            @RequestParam(required = false) String certificationNo) {
        try {
            // 验证状态值
            if (!"approved".equals(status) && !"rejected".equals(status)) {
                return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), "状态值只能为approved（已通过）或rejected（已拒绝）");
            }

            CoachCertificationReviewDTO reviewDTO = new CoachCertificationReviewDTO();
            reviewDTO.setId(id);
            reviewDTO.setStatus(status);
            reviewDTO.setReviewRemark(reviewRemark);
            reviewDTO.setCertificationNo(certificationNo);

            boolean success = adminCoachCertificationService.reviewApplication(reviewDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "审核教练认证申请失败");
            }
        } catch (RuntimeException e) {
            log.error("审核教练认证申请失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("审核教练认证申请失败（表单版）", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "审核教练认证申请失败：" + e.getMessage());
        }
    }

    /**
     * 批准教练认证申请（快捷操作）
     */
    @Operation(summary = "批准教练认证申请", description = "快捷批准教练认证申请")
    @PutMapping("/{id}/approve")
    public Result<Void> approveApplication(
            @Parameter(description = "申请ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "审核备注")
            @RequestParam(required = false) String reviewRemark) {
        try {
            CoachCertificationReviewDTO reviewDTO = new CoachCertificationReviewDTO();
            reviewDTO.setId(id);
            reviewDTO.setStatus("approved");
            reviewDTO.setReviewRemark(reviewRemark != null ? reviewRemark : "申请材料齐全，符合教练认证要求");

            boolean success = adminCoachCertificationService.reviewApplication(reviewDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "批准教练认证申请失败");
            }
        } catch (RuntimeException e) {
            log.error("批准教练认证申请失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("批准教练认证申请失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "批准教练认证申请失败：" + e.getMessage());
        }
    }

    /**
     * 拒绝教练认证申请（快捷操作）
     */
    @Operation(summary = "拒绝教练认证申请", description = "快捷拒绝教练认证申请")
    @PutMapping("/{id}/reject")
    public Result<Void> rejectApplication(
            @Parameter(description = "申请ID", required = true)
            @PathVariable @NotNull @Min(1) Long id,
            @Parameter(description = "拒绝原因", required = true)
            @RequestParam @NotNull String reviewRemark) {
        try {
            CoachCertificationReviewDTO reviewDTO = new CoachCertificationReviewDTO();
            reviewDTO.setId(id);
            reviewDTO.setStatus("rejected");
            reviewDTO.setReviewRemark(reviewRemark);

            boolean success = adminCoachCertificationService.reviewApplication(reviewDTO);
            if (success) {
                return Result.ok();
            } else {
                return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "拒绝教练认证申请失败");
            }
        } catch (RuntimeException e) {
            log.error("拒绝教练认证申请失败: {}", e.getMessage());
            return Result.fail(ResultCodeEnum.DATA_ERROR.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("拒绝教练认证申请失败", e);
            return Result.fail(ResultCodeEnum.SERVICE_ERROR.getCode(), "拒绝教练认证申请失败：" + e.getMessage());
        }
    }
}