package com.huang.web.admin.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.common.utils.JwtUtil;
import com.huang.web.admin.dto.coach.AdminCoachScheduleQueryDTO;
import com.huang.web.admin.dto.coach.CoachScheduleReviewDTO;
import com.huang.web.admin.mapper.AdminCoachScheduleChangeMapper;
import com.huang.model.entity.CoachScheduleChange;
import com.huang.model.entity.User;
import com.huang.web.admin.service.AdminCoachScheduleChangeService;
import com.huang.web.admin.service.UserService;
import com.huang.web.admin.vo.AdminCoachScheduleChangeDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Admin端教练日程审核Service实现
 * @author huang
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminCoachScheduleChangeServiceImpl extends ServiceImpl<AdminCoachScheduleChangeMapper, CoachScheduleChange> implements AdminCoachScheduleChangeService {

    private final UserService userService;

    @Override
    public IPage<AdminCoachScheduleChangeDetailVO> getCoachScheduleChangePageForAdmin(AdminCoachScheduleQueryDTO query) {
        log.info("分页查询教练日程变更申请，参数：{}", query);

        // 分页查询
        Page<AdminCoachScheduleChangeDetailVO> page = new Page<>(query.getCurrent(), query.getSize());
        return baseMapper.selectCoachScheduleChangePageForAdmin(page, query);
    }

    @Override
    public AdminCoachScheduleChangeDetailVO getCoachScheduleChangeDetailForAdmin(Long id) {
        log.info("查询教练日程变更申请详情，ID：{}", id);

        // 查询申请详情
        AdminCoachScheduleChangeDetailVO detail = baseMapper.selectCoachScheduleChangeDetailForAdmin(id);
        if (detail == null) {
            throw new RuntimeException("申请记录不存在");
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewCoachScheduleChange(String token, CoachScheduleReviewDTO dto) {
        log.info("审核教练日程变更申请，参数：{}", dto);

        // 验证token并获取用户信息
        Long userId = JwtUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 验证审核结果
        if (!"approved".equals(dto.getStatus()) && !"rejected".equals(dto.getStatus())) {
            throw new RuntimeException("审核结果只能是approved或rejected");
        }

        // 查询申请记录
        CoachScheduleChange scheduleChange = getById(dto.getId());
        if (scheduleChange == null) {
            throw new RuntimeException("申请记录不存在");
        }

        // 只有待审核状态的申请才能审核
        if (!"pending".equals(scheduleChange.getStatus())) {
            throw new RuntimeException("只有待审核状态的申请才能审核");
        }

        // 更新审核信息
        scheduleChange.setStatus(dto.getStatus());
        scheduleChange.setReviewTime(LocalDateTime.now());
        scheduleChange.setReviewerId(userId);
        scheduleChange.setReviewRemark(dto.getReviewRemark());

        if (!updateById(scheduleChange)) {
            throw new RuntimeException("审核申请失败");
        }

        log.info("教练日程变更申请审核成功，申请ID：{}，审核结果：{}", dto.getId(), dto.getStatus());
    }
}