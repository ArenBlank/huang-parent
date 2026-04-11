package com.huang.web.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huang.common.exception.GlobalExceptionHandler;
import com.huang.web.admin.service.biz.AdminTrainingPlanBizService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TrainingPlanManageControllerTest {

    @Mock
    private AdminTrainingPlanBizService adminTrainingPlanBizService;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TrainingPlanManageController(adminTrainingPlanBizService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void update_shouldReturnBizCode200WhenPayloadIsValid() throws Exception {
        when(adminTrainingPlanBizService.updatePlan(eq(1L), org.mockito.ArgumentMatchers.any())).thenReturn(true);

        mockMvc.perform(put("/admin/training-plan/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "Plan Update",
                                "goal", "fat-loss",
                                "level", "beginner",
                                "durationWeeks", 4,
                                "coverUrl", "https://example.com/plan.jpg",
                                "status", 1
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("成功"))
                .andExpect(jsonPath("$.data").value("更新成功"));

        verify(adminTrainingPlanBizService).updatePlan(eq(1L), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void update_shouldReturnParamErrorInsteadOfGenericFailWhenJsonMalformed() throws Exception {
        mockMvc.perform(put("/admin/training-plan/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"broken-json\""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(202))
                .andExpect(jsonPath("$.message").value("请求体格式错误"));
    }
}
