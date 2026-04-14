package com.huang.web.app.dto.plan;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiPlanGenerateDTO {

    @NotBlank(message = "请输入训练需求")
    private String userPrompt;
}
