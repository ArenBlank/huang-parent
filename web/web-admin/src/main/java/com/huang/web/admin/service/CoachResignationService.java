package com.huang.web.admin.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.common.login.LoginUserHolder;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.huang.model.entity.CoachResignationApply;
import com.huang.web.admin.dto.coach.CoachResignationQueryDTO;
import com.huang.web.admin.dto.coach.CoachResignationReviewDTO;
import com.huang.web.admin.mapper.CoachResignationApplyMapper;
import com.huang.web.admin.vo.coach.CoachResignationDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Admin端教练离职申请Service
 * @author system
 * @since 2025-01-24
 */
@Slf4j
@Service
public class CoachResignationService {

    @Autowired
    private CoachResignationApplyMapper resignationApplyMapper;

    /**
     * 分页查询教练离职申请列表
     */
    public IPage<CoachResignationDetailVO> getResignationApplications(CoachResignationQueryDTO queryDTO) {
        Page<CoachResignationDetailVO> page = new Page<>(queryDTO.getCurrent(), queryDTO.getSize());
        return resignationApplyMapper.selectResignationPage(page, queryDTO);
    }

    /**
     * 查看教练离职申请详情
     */
    public CoachResignationDetailVO getResignationDetail(Long id) {
        return resignationApplyMapper.selectResignationDetail(id);
    }

    /**
     * 批准离职申请
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean approveResignation(Long id, CoachResignationReviewDTO reviewDTO) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();

        // 检查申请是否存在且状态为待审核
        CoachResignationApply application = resignationApplyMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("离职申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new RuntimeException("只能审核待审核状态的申请");
        }

        // 更新申请状态
        LambdaUpdateWrapper<CoachResignationApply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachResignationApply::getId, id)
                    .set(CoachResignationApply::getStatus, "approved")
                    .set(CoachResignationApply::getReviewTime, LocalDateTime.now())
                    .set(CoachResignationApply::getReviewerId, currentUserId)
                    .set(CoachResignationApply::getReviewRemark, reviewDTO.getReviewRemark())
                    .set(CoachResignationApply::getUpdateTime, LocalDateTime.now());

        if (reviewDTO.getActualLeaveDate() != null) {
            updateWrapper.set(CoachResignationApply::getActualLeaveDate, reviewDTO.getActualLeaveDate());
        }

        boolean result = resignationApplyMapper.update(null, updateWrapper) > 0;
        log.info("批准教练离职申请成功: applicationId={}, reviewerId={}", id, currentUserId);

        return result;
    }

    /**
     * 拒绝离职申请
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean rejectResignation(Long id, CoachResignationReviewDTO reviewDTO) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();

        // 检查申请是否存在且状态为待审核
        CoachResignationApply application = resignationApplyMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("离职申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new RuntimeException("只能审核待审核状态的申请");
        }

        // 更新申请状态
        LambdaUpdateWrapper<CoachResignationApply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachResignationApply::getId, id)
                    .set(CoachResignationApply::getStatus, "rejected")
                    .set(CoachResignationApply::getReviewTime, LocalDateTime.now())
                    .set(CoachResignationApply::getReviewerId, currentUserId)
                    .set(CoachResignationApply::getReviewRemark, reviewDTO.getReviewRemark())
                    .set(CoachResignationApply::getUpdateTime, LocalDateTime.now());

        boolean result = resignationApplyMapper.update(null, updateWrapper) > 0;
        log.info("拒绝教练离职申请成功: applicationId={}, reviewerId={}", id, currentUserId);

        return result;
    }

    /**
     * 取消离职申请（管理员操作）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelResignation(Long id, String cancelReason) {
        Long currentUserId = LoginUserHolder.getLoginUser().getUserId();

        // 检查申请是否存在
        CoachResignationApply application = resignationApplyMapper.selectById(id);
        if (application == null) {
            throw new RuntimeException("离职申请不存在");
        }
        if (!"pending".equals(application.getStatus())) {
            throw new RuntimeException("只能取消待审核状态的申请");
        }

        // 更新申请状态
        LambdaUpdateWrapper<CoachResignationApply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CoachResignationApply::getId, id)
                    .set(CoachResignationApply::getStatus, "cancelled")
                    .set(CoachResignationApply::getReviewTime, LocalDateTime.now())
                    .set(CoachResignationApply::getReviewerId, currentUserId)
                    .set(CoachResignationApply::getReviewRemark, cancelReason != null ? cancelReason : "管理员取消申请")
                    .set(CoachResignationApply::getUpdateTime, LocalDateTime.now());

        boolean result = resignationApplyMapper.update(null, updateWrapper) > 0;
        log.info("取消教练离职申请成功: applicationId={}, reviewerId={}", id, currentUserId);

        return result;
    }
}