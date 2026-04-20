package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.common.exception.HuangException;
import com.huang.model.entity.CoachProfile;
import com.huang.model.entity.CoachSchedule;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.dto.coach.CoachScheduleUpsertDTO;
import com.huang.web.admin.mapper.CoachProfileMapper;
import com.huang.web.admin.mapper.CoachScheduleMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

@Service
public class AdminCoachScheduleBizService {

    private static final int CERT_APPROVED = 1;
    private static final int STATUS_DISABLED = 0;
    private static final int STATUS_ENABLED = 1;

    private final CoachScheduleMapper coachScheduleMapper;
    private final CoachProfileMapper coachProfileMapper;

    public AdminCoachScheduleBizService(CoachScheduleMapper coachScheduleMapper,
                                        CoachProfileMapper coachProfileMapper) {
        this.coachScheduleMapper = coachScheduleMapper;
        this.coachProfileMapper = coachProfileMapper;
    }

    public List<CoachSchedule> listSchedules(Long coachId, LocalDate scheduleDate, Integer status) {
        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<CoachSchedule>()
                .orderByAsc(CoachSchedule::getScheduleDate, CoachSchedule::getStartTime, CoachSchedule::getId);
        if (coachId != null) {
            wrapper.eq(CoachSchedule::getCoachId, coachId);
        }
        if (scheduleDate != null) {
            wrapper.eq(CoachSchedule::getScheduleDate, scheduleDate);
        }
        if (status != null) {
            wrapper.eq(CoachSchedule::getStatus, status);
        }
        return coachScheduleMapper.selectList(wrapper);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createSchedule(CoachScheduleUpsertDTO dto) {
        validateStatus(dto.getStatus());
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        assertCoachApproved(dto.getCoachId());
        assertNoOverlap(null, dto);

        CoachSchedule schedule = new CoachSchedule();
        fillSchedule(schedule, dto);
        schedule.setBookedCount(0);
        coachScheduleMapper.insert(schedule);
        return schedule.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateSchedule(Long id, CoachScheduleUpsertDTO dto) {
        CoachSchedule exists = coachScheduleMapper.selectById(id);
        if (exists == null) {
            return false;
        }

        validateStatus(dto.getStatus());
        validateTimeRange(dto.getStartTime(), dto.getEndTime());
        assertCoachApproved(dto.getCoachId());

        if (hasBookedCount(exists) && hasManagedFieldChanges(exists, dto)) {
            throw new HuangException(
                    AdminErrorCode.COACH_SCHEDULE_UPDATE_FORBIDDEN,
                    "已有预约记录的教练档期不允许编辑，请使用状态开关控制启停"
            );
        }

        assertNoOverlap(id, dto);

        fillSchedule(exists, dto);
        return coachScheduleMapper.updateById(exists) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean updateScheduleStatus(Long id, Integer status) {
        validateStatus(status);
        CoachSchedule exists = coachScheduleMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        exists.setStatus(status);
        return coachScheduleMapper.updateById(exists) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteSchedule(Long id) {
        CoachSchedule exists = coachScheduleMapper.selectById(id);
        if (exists == null) {
            return false;
        }
        if (hasBookedCount(exists)) {
            throw new HuangException(AdminErrorCode.COACH_SCHEDULE_DELETE_FORBIDDEN, "已有预约记录的教练档期不允许删除");
        }
        return coachScheduleMapper.deleteById(id) > 0;
    }

    private void fillSchedule(CoachSchedule schedule, CoachScheduleUpsertDTO dto) {
        schedule.setCoachId(dto.getCoachId());
        schedule.setScheduleDate(dto.getScheduleDate());
        schedule.setStartTime(dto.getStartTime());
        schedule.setEndTime(dto.getEndTime());
        schedule.setPrice(dto.getPrice());
        schedule.setCapacity(dto.getCapacity());
        schedule.setStatus(dto.getStatus());
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            throw new HuangException(AdminErrorCode.COACH_SCHEDULE_CONFLICT, "结束时间必须晚于开始时间");
        }
    }

    private void validateStatus(Integer status) {
        if (!Objects.equals(status, STATUS_DISABLED) && !Objects.equals(status, STATUS_ENABLED)) {
            throw new HuangException(AdminErrorCode.COACH_SCHEDULE_STATUS_INVALID, "教练档期状态仅支持 0/1");
        }
    }

    private void assertCoachApproved(Long coachId) {
        CoachProfile profile = coachProfileMapper.selectById(coachId);
        if (profile == null || !Objects.equals(profile.getCertStatus(), CERT_APPROVED)) {
            throw new HuangException(AdminErrorCode.COACH_SCHEDULE_COACH_INVALID, "教练不存在或未审核通过");
        }
    }

    private void assertNoOverlap(Long excludeId, CoachScheduleUpsertDTO dto) {
        LambdaQueryWrapper<CoachSchedule> wrapper = new LambdaQueryWrapper<CoachSchedule>()
                .eq(CoachSchedule::getCoachId, dto.getCoachId())
                .eq(CoachSchedule::getScheduleDate, dto.getScheduleDate())
                .lt(CoachSchedule::getStartTime, dto.getEndTime())
                .gt(CoachSchedule::getEndTime, dto.getStartTime());
        if (excludeId != null) {
            wrapper.ne(CoachSchedule::getId, excludeId);
        }
        Long count = coachScheduleMapper.selectCount(wrapper);
        if (count != null && count > 0) {
            throw new HuangException(AdminErrorCode.COACH_SCHEDULE_CONFLICT, "同一教练在该日期下存在时间重叠的档期");
        }
    }

    private boolean hasBookedCount(CoachSchedule schedule) {
        return schedule.getBookedCount() != null && schedule.getBookedCount() > 0;
    }

    private boolean hasManagedFieldChanges(CoachSchedule exists, CoachScheduleUpsertDTO dto) {
        return !Objects.equals(exists.getCoachId(), dto.getCoachId())
                || !Objects.equals(exists.getScheduleDate(), dto.getScheduleDate())
                || !Objects.equals(exists.getStartTime(), dto.getStartTime())
                || !Objects.equals(exists.getEndTime(), dto.getEndTime())
                || !Objects.equals(exists.getPrice(), dto.getPrice())
                || !Objects.equals(exists.getCapacity(), dto.getCapacity())
                || !Objects.equals(exists.getStatus(), dto.getStatus());
    }
}
