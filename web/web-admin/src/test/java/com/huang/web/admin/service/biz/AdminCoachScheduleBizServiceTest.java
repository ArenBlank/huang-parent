package com.huang.web.admin.service.biz;

import com.huang.common.exception.HuangException;
import com.huang.model.entity.CoachProfile;
import com.huang.model.entity.CoachSchedule;
import com.huang.web.admin.dto.coach.CoachScheduleUpsertDTO;
import com.huang.web.admin.mapper.CoachProfileMapper;
import com.huang.web.admin.mapper.CoachScheduleMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCoachScheduleBizServiceTest {

    @Mock
    private CoachScheduleMapper coachScheduleMapper;

    @Mock
    private CoachProfileMapper coachProfileMapper;

    @InjectMocks
    private AdminCoachScheduleBizService adminCoachScheduleBizService;

    @Test
    void createSchedule_shouldInsertBookedCountAsZeroWhenValid() {
        CoachProfile coachProfile = new CoachProfile();
        coachProfile.setId(2L);
        coachProfile.setCertStatus(1);
        when(coachProfileMapper.selectById(2L)).thenReturn(coachProfile);
        when(coachScheduleMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            CoachSchedule schedule = invocation.getArgument(0);
            schedule.setId(11L);
            return 1;
        }).when(coachScheduleMapper).insert(any(CoachSchedule.class));

        Long id = adminCoachScheduleBizService.createSchedule(validDto());

        ArgumentCaptor<CoachSchedule> captor = ArgumentCaptor.forClass(CoachSchedule.class);
        verify(coachScheduleMapper).insert(captor.capture());
        CoachSchedule inserted = captor.getValue();
        assertEquals(0, inserted.getBookedCount());
        assertEquals(11L, id);
    }

    @Test
    void createSchedule_shouldThrowWhenCoachNotApproved() {
        CoachProfile coachProfile = new CoachProfile();
        coachProfile.setId(2L);
        coachProfile.setCertStatus(0);
        when(coachProfileMapper.selectById(2L)).thenReturn(coachProfile);

        assertThrows(HuangException.class, () -> adminCoachScheduleBizService.createSchedule(validDto()));
        verify(coachScheduleMapper, never()).insert(any());
    }

    @Test
    void createSchedule_shouldThrowWhenTimeOverlapExists() {
        CoachProfile coachProfile = new CoachProfile();
        coachProfile.setId(2L);
        coachProfile.setCertStatus(1);
        when(coachProfileMapper.selectById(2L)).thenReturn(coachProfile);
        when(coachScheduleMapper.selectCount(any())).thenReturn(1L);

        assertThrows(HuangException.class, () -> adminCoachScheduleBizService.createSchedule(validDto()));
        verify(coachScheduleMapper, never()).insert(any());
    }

    @Test
    void updateSchedule_shouldThrowWhenBookedScheduleChangesCoreFields() {
        CoachProfile coachProfile = new CoachProfile();
        coachProfile.setId(2L);
        coachProfile.setCertStatus(1);
        when(coachProfileMapper.selectById(2L)).thenReturn(coachProfile);

        CoachSchedule exists = new CoachSchedule();
        exists.setId(8L);
        exists.setCoachId(2L);
        exists.setScheduleDate(LocalDate.of(2026, 4, 20));
        exists.setStartTime(LocalTime.of(18, 0));
        exists.setEndTime(LocalTime.of(19, 0));
        exists.setPrice(new BigDecimal("199"));
        exists.setCapacity(3);
        exists.setBookedCount(1);
        exists.setStatus(1);
        when(coachScheduleMapper.selectById(8L)).thenReturn(exists);

        CoachScheduleUpsertDTO dto = validDto();
        dto.setCapacity(5);

        assertThrows(HuangException.class, () -> adminCoachScheduleBizService.updateSchedule(8L, dto));
        verify(coachScheduleMapper, never()).updateById(any());
    }

    @Test
    void deleteSchedule_shouldThrowWhenBookedCountGreaterThanZero() {
        CoachSchedule exists = new CoachSchedule();
        exists.setId(8L);
        exists.setBookedCount(2);
        when(coachScheduleMapper.selectById(8L)).thenReturn(exists);

        assertThrows(HuangException.class, () -> adminCoachScheduleBizService.deleteSchedule(8L));
        verify(coachScheduleMapper, never()).deleteById(any());
    }

    @Test
    void listSchedules_shouldReturnMapperResult() {
        CoachSchedule item = new CoachSchedule();
        item.setId(1L);
        when(coachScheduleMapper.selectList(any())).thenReturn(List.of(item));

        List<CoachSchedule> result = adminCoachScheduleBizService.listSchedules(2L, LocalDate.of(2026, 4, 20), 1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(coachScheduleMapper).selectList(any());
    }

    private CoachScheduleUpsertDTO validDto() {
        CoachScheduleUpsertDTO dto = new CoachScheduleUpsertDTO();
        dto.setCoachId(2L);
        dto.setScheduleDate(LocalDate.of(2026, 4, 20));
        dto.setStartTime(LocalTime.of(18, 0));
        dto.setEndTime(LocalTime.of(19, 0));
        dto.setPrice(new BigDecimal("199"));
        dto.setCapacity(3);
        dto.setStatus(1);
        return dto;
    }
}
