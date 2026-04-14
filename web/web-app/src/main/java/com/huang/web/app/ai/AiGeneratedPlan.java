package com.huang.web.app.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiGeneratedPlan(
        String planName,
        String description,
        String difficulty,
        Integer durationWeeks,
        List<AiGeneratedPlanItem> items
) {
}
