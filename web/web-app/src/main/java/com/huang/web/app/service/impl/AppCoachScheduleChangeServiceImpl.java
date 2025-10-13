package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.web.app.dto.CoachScheduleChangeDTO;
import com.huang.web.app.dto.CoachScheduleQueryDTO;
import com.huang.web.app.mapper.AppCoachScheduleChangeMapper;
import com.huang.model.entity.Coach;
import com.huang.model.entity.CoachScheduleChange;
import com.huang.web.app.service.AppCoachScheduleChangeService;
import com.huang.web.app.service.CoachService;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.web.app.vo.coach.CoachScheduleChangeDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * App端教练日程变更Service实现
 * @author huang
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppCoachScheduleChangeServiceImpl extends ServiceImpl<AppCoachScheduleChangeMapper, CoachScheduleChange> implements AppCoachScheduleChangeService {

    private final CoachService coachService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void applyScheduleChange(String token, CoachScheduleChangeDTO dto) {
        log.info("教练申请日程变更，参数：{}", dto);

        // 从ThreadLocal获取当前登录用户
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        Long userId = loginUser.getUserId();

        // 获取教练信息
        Coach coach = coachService.getCoachByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您不是认证教练，无法申请日程变更");
        }

        // 创建日程变更申请
        CoachScheduleChange scheduleChange = new CoachScheduleChange();
        BeanUtils.copyProperties(dto, scheduleChange);
        scheduleChange.setCoachId(coach.getId());
        scheduleChange.setStatus("pending");
        scheduleChange.setApplyTime(LocalDateTime.now());

        // 保存申请记录
        if (!save(scheduleChange)) {
            throw new RuntimeException("申请日程变更失败");
        }

        log.info("教练日程变更申请成功，申请ID：{}", scheduleChange.getId());
    }

    @Override
    public IPage<CoachScheduleChangeDetailVO> getScheduleChangePage(String token, CoachScheduleQueryDTO query) {
        log.info("分页查询教练日程变更申请，参数：{}", query);

        // 从ThreadLocal获取当前登录用户
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        Long userId = loginUser.getUserId();

        // 获取教练信息
        Coach coach = coachService.getCoachByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您不是认证教练");
        }

        // 分页查询
        Page<CoachScheduleChangeDetailVO> page = new Page<>(query.getCurrent(), query.getSize());
        return baseMapper.selectCoachScheduleChangePage(page, coach.getId(), query);
    }

    @Override
    public CoachScheduleChangeDetailVO getScheduleChangeDetail(String token, Long id) {
        log.info("查询教练日程变更申请详情，ID：{}", id);

        // 从ThreadLocal获取当前登录用户
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        Long userId = loginUser.getUserId();

        // 获取教练信息
        Coach coach = coachService.getCoachByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您不是认证教练");
        }

        // 查询申请详情
        CoachScheduleChangeDetailVO detail = baseMapper.selectCoachScheduleChangeDetail(id, coach.getId());
        if (detail == null) {
            throw new RuntimeException("申请记录不存在或无权限查看");
        }

        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelScheduleChange(String token, Long id) {
        log.info("取消教练日程变更申请，ID：{}", id);

        // 从ThreadLocal获取当前登录用户
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        Long userId = loginUser.getUserId();

        // 获取教练信息
        Coach coach = coachService.getCoachByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您不是认证教练");
        }

        // 查询申请记录
        CoachScheduleChange scheduleChange = getById(id);
        if (scheduleChange == null || !scheduleChange.getCoachId().equals(coach.getId())) {
            throw new RuntimeException("申请记录不存在或无权限操作");
        }

        // 只有待审核状态的申请才能取消
        if (!"pending".equals(scheduleChange.getStatus())) {
            throw new RuntimeException("只有待审核状态的申请才能取消");
        }

        // 标记为已删除
        scheduleChange.setIsDeleted((byte) 1);
        if (!updateById(scheduleChange)) {
            throw new RuntimeException("取消申请失败");
        }

        log.info("教练日程变更申请已取消，ID：{}", id);
    }
}
