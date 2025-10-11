package com.huang.web.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.common.result.Result;
import com.huang.model.entity.User;
import com.huang.model.entity.UserAccountCancelApply;
import com.huang.web.admin.dto.user.AccountCancelReviewDTO;
import com.huang.web.admin.mapper.UserAccountCancelApplyMapper;
import com.huang.web.admin.mapper.UserMapper;
import com.huang.web.admin.vo.user.AccountCancelApplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin端用户账号注销申请管理控制器
 * @author system
 * @since 2025-01-24
 */
@Tag(name = "用户账号注销管理", description = "管理员审核用户账号注销申请")
@Slf4j
@RestController
@RequestMapping("/admin/user-account-cancel")
@Validated
public class UserAccountCancelController {

    @Autowired
    private UserAccountCancelApplyMapper cancelApplyMapper;

    @Autowired
    private UserMapper userMapper;

    @Operation(summary = "获取注销申请列表", description = "分页查询用户账号注销申请列表")
    @GetMapping("/list")
    public Result<IPage<AccountCancelApplyVO>> getApplyList(
            @Parameter(description = "页码", example = "1") @RequestParam(defaultValue = "1") Integer pageNum,
            @Parameter(description = "每页数量", example = "10") @RequestParam(defaultValue = "10") Integer pageSize,
            @Parameter(description = "状态筛选: pending待审核 approved已批准 rejected已拒绝 cancelled已取消") @RequestParam(required = false) String status,
            @Parameter(description = "用户名搜索") @RequestParam(required = false) String username,
            @Parameter(description = "手机号搜索") @RequestParam(required = false) String phone) {
        
        log.info("获取注销申请列表: pageNum={}, pageSize={}, status={}, username={}, phone={}", 
                pageNum, pageSize, status, username, phone);

        // 构建查询条件
        LambdaQueryWrapper<UserAccountCancelApply> queryWrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.trim().isEmpty()) {
            queryWrapper.eq(UserAccountCancelApply::getStatus, status);
        }
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like(UserAccountCancelApply::getUsername, username);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            queryWrapper.like(UserAccountCancelApply::getPhone, phone);
        }
        
        queryWrapper.orderByDesc(UserAccountCancelApply::getApplyTime);

        // 分页查询
        Page<UserAccountCancelApply> page = new Page<>(pageNum, pageSize);
        IPage<UserAccountCancelApply> applyPage = cancelApplyMapper.selectPage(page, queryWrapper);

        // 转换为VO
        IPage<AccountCancelApplyVO> voPage = applyPage.convert(this::convertToVO);

        log.info("获取注销申请列表成功: 总数={}", voPage.getTotal());
        return Result.ok(voPage);
    }

    @Operation(summary = "获取注销申请详情", description = "根据ID获取单个注销申请的详细信息")
    @GetMapping("/{id}")
    public Result<AccountCancelApplyVO> getApplyDetail(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id) {
        
        log.info("获取注销申请详情: id={}", id);

        UserAccountCancelApply apply = cancelApplyMapper.selectById(id);
        if (apply == null) {
            return Result.fail("申请记录不存在");
        }

        AccountCancelApplyVO vo = convertToVO(apply);

        log.info("获取注销申请详情成功: id={}", id);
        return Result.ok(vo);
    }

    @Operation(summary = "审核注销申请", description = "管理员审核用户账号注销申请")
    @PutMapping("/{id}/review")
    public Result<String> reviewApply(
            @Parameter(description = "申请ID", required = true) @PathVariable Long id,
            @Valid @RequestBody AccountCancelReviewDTO dto) {
        
        LoginUser currentAdmin = LoginUserHolder.getLoginUser();
        log.info("审核注销申请: id={}, reviewStatus={}, adminId={}", 
                id, dto.getReviewStatus(), currentAdmin.getUserId());

        // 1. 查询申请记录
        UserAccountCancelApply apply = cancelApplyMapper.selectById(id);
        if (apply == null) {
            return Result.fail("申请记录不存在");
        }

        // 2. 验证状态
        if (!"pending".equals(apply.getStatus())) {
            return Result.fail("该申请已被处理，无法重复审核");
        }

        // 3. 更新审核信息
        apply.setStatus(dto.getReviewStatus());
        apply.setReviewTime(LocalDateTime.now());
        apply.setReviewerId(currentAdmin.getUserId());
        apply.setReviewRemark(dto.getReviewRemark());

        // 4. 如果批准，设置实际注销时间
        if ("approved".equals(dto.getReviewStatus())) {
            apply.setActualCancelTime(apply.getEffectiveTime());
            
            // TODO: 这里可以添加实际的账号注销逻辑
            // 比如：禁用用户账号、清理用户数据等
            // User user = userMapper.selectById(apply.getUserId());
            // user.setStatus(0); // 禁用账号
            // userMapper.updateById(user);
        }

        int result = cancelApplyMapper.updateById(apply);
        if (result <= 0) {
            return Result.fail("审核失败，请稍后重试");
        }

        log.info("审核注销申请成功: id={}, reviewStatus={}", id, dto.getReviewStatus());
        return Result.ok("审核成功");
    }

    @Operation(summary = "获取待审核数量", description = "获取待审核的注销申请数量")
    @GetMapping("/pending-count")
    public Result<Long> getPendingCount() {
        LambdaQueryWrapper<UserAccountCancelApply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserAccountCancelApply::getStatus, "pending");
        
        Long count = cancelApplyMapper.selectCount(queryWrapper);
        
        log.info("获取待审核数量: count={}", count);
        return Result.ok(count);
    }

    /**
     * 将实体转换为VO
     */
    private AccountCancelApplyVO convertToVO(UserAccountCancelApply apply) {
        AccountCancelApplyVO vo = new AccountCancelApplyVO();
        BeanUtils.copyProperties(apply, vo);

        // 设置描述字段
        vo.setCancelTypeDesc("temporary".equals(apply.getCancelType()) ? "临时注销" : "永久注销");
        
        String statusDesc = "";
        switch (apply.getStatus()) {
            case "pending":
                statusDesc = "待审核";
                break;
            case "approved":
                statusDesc = "已批准";
                break;
            case "rejected":
                statusDesc = "已拒绝";
                break;
            case "cancelled":
                statusDesc = "已取消";
                break;
            default:
                statusDesc = "未知";
        }
        vo.setStatusDesc(statusDesc);

        // 如果有审核人，获取审核人姓名
        if (apply.getReviewerId() != null) {
            User reviewer = userMapper.selectById(apply.getReviewerId());
            if (reviewer != null) {
                vo.setReviewerName(reviewer.getUsername());
            }
        }

        return vo;
    }
}