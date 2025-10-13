package com.huang.web.app.service.impl;

import com.huang.common.login.LoginUserHolder;
import com.huang.model.entity.Coach;
import com.huang.model.entity.CoachConsultation;
import com.huang.web.app.dto.coach.CoachConsultationBookDTO;
import com.huang.web.app.mapper.CoachConsultationMapper;
import com.huang.web.app.mapper.CoachMapper;
import com.huang.web.app.service.CoachConsultationService;
import com.huang.web.app.vo.coach.CoachConsultationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * App端教练咨询Service实现类
 * @author system
 * @since 2025-01-25
 */
@Service
@Slf4j
public class CoachConsultationServiceImpl implements CoachConsultationService {

    @Autowired
    private CoachConsultationMapper coachConsultationMapper;

    @Autowired
    private CoachMapper coachMapper;

    /**
     * 预约咨询
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bookConsultation(CoachConsultationBookDTO bookDTO) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        log.info("用户ID:{} 预约教练咨询, 教练ID:{}", userId, bookDTO.getCoachId());

        // 1. 验证教练是否存在
        Coach coach = coachMapper.selectById(bookDTO.getCoachId());
        if (coach == null) {
            throw new RuntimeException("教练不存在");
        }

        // 检查教练状态
        if (coach.getStatus() != null && coach.getStatus() == 0) {
            throw new RuntimeException("该教练已离职，无法预约");
        }

        // 2. 检查时间冲突
        int conflictCount = coachConsultationMapper.checkTimeConflict(
            bookDTO.getCoachId(),
            bookDTO.getConsultationDate(),
            bookDTO.getDuration()
        );

        if (conflictCount > 0) {
            throw new RuntimeException("该时间段教练已有其他咨询安排，请选择其他时间");
        }

        // 3. 创建咨询记录
        CoachConsultation consultation = new CoachConsultation();
        BeanUtils.copyProperties(bookDTO, consultation);
        consultation.setUserId(userId);
        consultation.setStatus("scheduled");
        consultation.setIsDeleted((byte) 0);

        coachConsultationMapper.insert(consultation);
        log.info("教练咨询预约成功, 咨询ID:{}", consultation.getId());

        return consultation.getId();
    }

    /**
     * 查询我的咨询记录
     */
    @Override
    public List<CoachConsultationVO> getMyConsultations(String status) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        log.info("查询用户ID:{} 的咨询记录, 状态:{}", userId, status);

        return coachConsultationMapper.selectMyConsultations(userId, status);
    }

    /**
     * 获取咨询详情
     */
    @Override
    public CoachConsultationVO getConsultationDetail(Long id) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        log.info("查询咨询详情, ID:{}, 用户ID:{}", id, userId);

        CoachConsultationVO detail = coachConsultationMapper.selectConsultationByIdAndUserId(id, userId);
        if (detail == null) {
            throw new RuntimeException("咨询记录不存在");
        }

        return detail;
    }

    /**
     * 取消咨询
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancelConsultation(Long id) {
        Long userId = LoginUserHolder.getLoginUser().getUserId();
        log.info("用户ID:{} 取消咨询, 咨询ID:{}", userId, id);

        // 1. 查询咨询记录
        CoachConsultationVO consultation = coachConsultationMapper.selectConsultationByIdAndUserId(id, userId);
        if (consultation == null) {
            throw new RuntimeException("咨询记录不存在");
        }

        // 2. 检查是否可以取消
        if (!"scheduled".equals(consultation.getStatus())) {
            throw new RuntimeException("只有已预约状态的咨询才能取消");
        }

        // 检查是否已过期
        if (consultation.getConsultationDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("咨询时间已过，无法取消");
        }

        // 3. 更新状态为已取消
        CoachConsultation updateEntity = new CoachConsultation();
        updateEntity.setId(id);
        updateEntity.setStatus("cancelled");
        updateEntity.setUpdateTime(LocalDateTime.now());

        int rows = coachConsultationMapper.updateById(updateEntity);
        log.info("取消咨询完成, 影响行数:{}", rows);

        return rows > 0;
    }
}
