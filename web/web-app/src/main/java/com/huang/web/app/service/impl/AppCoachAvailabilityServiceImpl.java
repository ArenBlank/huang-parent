package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.web.app.dto.CoachAvailabilityDTO;
import com.huang.web.app.mapper.AppCoachAvailabilityMapper;
import com.huang.model.entity.Coach;
import com.huang.model.entity.CoachAvailability;
import com.huang.web.app.service.AppCoachAvailabilityService;
import com.huang.web.app.service.CoachService;
import com.huang.common.login.LoginUser;
import com.huang.common.login.LoginUserHolder;
import com.huang.web.app.vo.coach.CoachAvailabilityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * App端教练可用性Service实现
 * @author huang
 * @since 2025-01-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AppCoachAvailabilityServiceImpl extends ServiceImpl<AppCoachAvailabilityMapper, CoachAvailability> implements AppCoachAvailabilityService {

    private final CoachService coachService;

    @Override
    public List<CoachAvailabilityVO> getCoachAvailabilityList(String token) {
        log.info("查询教练工作时间列表");

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

        return baseMapper.selectCoachAvailabilityList(coach.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setCoachAvailability(String token, CoachAvailabilityDTO dto) {
        log.info("设置教练工作时间，参数：{}", dto);

        // 从ThreadLocal获取当前登录用户
        LoginUser loginUser = LoginUserHolder.getLoginUser();
        if (loginUser == null) {
            throw new RuntimeException("用户未登录");
        }
        Long userId = loginUser.getUserId();

        // 获取教练信息
        Coach coach = coachService.getCoachByUserId(userId);
        if (coach == null) {
            throw new RuntimeException("您不是认证教练，无法设置工作时间");
        }

        // 验证时间设置合理性
        if (dto.getStartTime().isAfter(dto.getEndTime()) || dto.getStartTime().equals(dto.getEndTime())) {
            throw new RuntimeException("开始时间必须早于结束时间");
        }

        // 查询是否已存在相同星期的设置
        CoachAvailability existing = baseMapper.selectByCoachIdAndDayOfWeek(coach.getId(), dto.getDayOfWeek());

        if (existing != null) {
            // 更新现有记录
            BeanUtils.copyProperties(dto, existing);
            existing.setCoachId(coach.getId());
            if (!updateById(existing)) {
                throw new RuntimeException("更新工作时间设置失败");
            }
        } else {
            // 创建新记录
            CoachAvailability availability = new CoachAvailability();
            BeanUtils.copyProperties(dto, availability);
            availability.setCoachId(coach.getId());
            if (!save(availability)) {
                throw new RuntimeException("设置工作时间失败");
            }
        }

        log.info("教练工作时间设置成功，教练ID：{}，星期：{}", coach.getId(), dto.getDayOfWeek());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCoachAvailability(String token, Long id) {
        log.info("删除教练工作时间设置，ID：{}", id);

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

        // 查询工作时间设置记录
        CoachAvailability availability = getById(id);
        if (availability == null || !availability.getCoachId().equals(coach.getId())) {
            throw new RuntimeException("工作时间设置不存在或无权限操作");
        }

        // 标记为已删除
        availability.setIsDeleted((byte) 1);
        if (!updateById(availability)) {
            throw new RuntimeException("删除工作时间设置失败");
        }

        log.info("教练工作时间设置已删除，ID：{}", id);
    }
}
