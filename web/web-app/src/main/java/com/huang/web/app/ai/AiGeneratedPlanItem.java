package com.huang.web.app.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AiGeneratedPlanItem(
        Integer dayIndex,
        String actionName,
        Integer sets,
        Integer reps,
        Integer durationMin,
        Integer restSec
) {
}
