package com.huang.web.app.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateBookingDTO {

    @NotNull
    @Min(1)
    private Long scheduleId;
}
