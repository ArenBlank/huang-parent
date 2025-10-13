package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.login.LoginUserHolder;
import com.huang.model.entity.Coach;
import com.huang.model.entity.CoachResignationApply;
import com.huang.web.app.dto.coach.CoachResignationApplyDTO;
import com.huang.web.app.mapper.CoachMapper;
import com.huang.web.app.mapper.CoachResignationApplyMapper;
import com.huang.web.app.service.CoachResignationService;
import com.huang.web.app.vo.coach.CoachResignationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * App端教练离职申请Service实现类
 * @author system
 * @since 2025-01-25
 */
@Service
@Slf4j
public class CoachResignationServiceImpl implements CoachResignationService {

    @Autowired
    private CoachResignationApplyMapper coachResignationApplyMapper;

    @Autowired
    private CoachMapper coachMapper;

    /**
     * 提交教练离职申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long applyResignation(CoachResignationApplyDTO applyDTO) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        log.info("用户ID:{} 提交教练离职申请", userId);

        // 1. 查询教练信息
        Coach coach = coachMapper.selectByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您还不是教练,无法申请离职");
        }

        // 检查教练状态
        if (coach.getStatus() != null && coach.getStatus() == 0) {
            throw new RuntimeException("您已经离职,无需重复申请");
        }

        // 2. 检查是否有待审核的离职申请
        CoachResignationVO pendingApplication = coachResignationApplyMapper.selectPendingByCoachId(coach.getId());
        if (pendingApplication != null) {
            throw new RuntimeException("您有正在审核中的离职申请,请勿重复提交");
        }

        // 3. 创建离职申请
        CoachResignationApply resignationApply = new CoachResignationApply();
        BeanUtils.copyProperties(applyDTO, resignationApply);
        resignationApply.setCoachId(coach.getId());
        resignationApply.setStatus("pending");
        resignationApply.setApplyTime(LocalDateTime.now());
        resignationApply.setIsDeleted((byte) 0);

        coachResignationApplyMapper.insert(resignationApply);
        log.info("教练离职申请创建成功, 申请ID:{}", resignationApply.getId());

        return resignationApply.getId();
    }

    /**
     * 获取当前教练的离职申请状态
     */
    @Override
    public CoachResignationVO getCurrentResignationStatus() {
        Long userId = LoginUserHolder.getLoginUser().getUserId();

        // 查询教练信息
        Coach coach = coachMapper.selectByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您还不是教练");
        }

        // 查询待审核的离职申请
        return coachResignationApplyMapper.selectPendingByCoachId(coach.getId());
    }

    /**
     * 获取离职申请历史记录
     */
    @Override
    public List<CoachResignationVO> getResignationHistory() {
        Long userId = LoginUserHolder.getLoginUser().getUserId();

        // 查询教练信息
        Coach coach = coachMapper.selectByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您还不是教练");
        }

        return coachResignationApplyMapper.selectHistoryByCoachId(coach.getId());
    }

    /**
     * 撤销离职申请
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelResignation(Long applicationId) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        log.info("用户ID:{} 撤销教练离职申请, 申请ID:{}", userId, applicationId);

        // 查询教练信息
        Coach coach = coachMapper.selectByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您还不是教练");
        }

        // 查询离职申请
        CoachResignationVO application = coachResignationApplyMapper.selectByIdAndCoachId(applicationId, coach.getId());
        if (application == null) {
            throw new RuntimeException("离职申请不存在");
        }

        // 检查是否可以撤销
        if (!application.getCanCancel()) {
            throw new RuntimeException("该离职申请不能撤销");
        }

        // 更新状态为已取消
        CoachResignationApply updateEntity = new CoachResignationApply();
        updateEntity.setId(applicationId);
        updateEntity.setStatus("cancelled");
        updateEntity.setUpdateTime(LocalDateTime.now());

        int rows = coachResignationApplyMapper.updateById(updateEntity);
        log.info("撤销教练离职申请完成, 影响行数:{}", rows);

        return rows > 0;
    }

    /**
     * 获取离职申请详情
     */
    @Override
    public CoachResignationVO getResignationDetail(Long applicationId) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();

        // 查询教练信息
        Coach coach = coachMapper.selectByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您还不是教练");
        }

        CoachResignationVO detail = coachResignationApplyMapper.selectByIdAndCoachId(applicationId, coach.getId());
        if (detail == null) {
            throw new RuntimeException("离职申请不存在");
        }

        return detail;
    }
}
