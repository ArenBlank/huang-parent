package com.huang.web.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.common.exception.GlobalExceptionHandler;
import com.huang.common.exception.HuangException;
import com.huang.web.admin.constant.AdminErrorCode;
import com.huang.web.admin.service.biz.AdminCoachScheduleBizService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminCoachScheduleControllerTest {

    @Mock
    private AdminCoachScheduleBizService adminCoachScheduleBizService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AdminCoachScheduleController(adminCoachScheduleBizService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void updateStatus_shouldReturnBizFailWhenStatusInvalid() throws Exception {
        mockMvc.perform(put("/admin/coach-schedule/1/status")
                        .param("status", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AdminErrorCode.COACH_SCHEDULE_STATUS_INVALID))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void create_shouldReturnBizCode200WhenPayloadValid() throws Exception {
        when(adminCoachScheduleBizService.createSchedule(org.mockito.ArgumentMatchers.any())).thenReturn(10L);

        mockMvc.perform(post("/admin/coach-schedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "coachId", 2,
                                "scheduleDate", "2026-04-20",
                                "startTime", "18:00:00",
                                "endTime", "19:00:00",
                                "price", 199,
                                "capacity", 3,
                                "status", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value(10));
    }

    @Test
    void delete_shouldBubbleBusinessExceptionViaGlobalHandler() throws Exception {
        when(adminCoachScheduleBizService.deleteSchedule(eq(9L)))
                .thenThrow(new HuangException(AdminErrorCode.COACH_SCHEDULE_DELETE_FORBIDDEN, "已有预约记录的教练档期不允许删除"));

        mockMvc.perform(delete("/admin/coach-schedule/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(AdminErrorCode.COACH_SCHEDULE_DELETE_FORBIDDEN))
                .andExpect(jsonPath("$.message").value("已有预约记录的教练档期不允许删除"));
    }
}
