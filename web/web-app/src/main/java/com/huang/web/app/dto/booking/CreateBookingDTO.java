package com.huang.web.app.dto.booking;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Coach booking create request")
public class CreateBookingDTO {

    @NotNull
    @Min(1)
    @Schema(description = "Coach schedule ID", example = "5")
    private Long scheduleId;
}
