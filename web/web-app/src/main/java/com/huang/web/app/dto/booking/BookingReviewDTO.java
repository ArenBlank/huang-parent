package com.huang.web.app.dto.booking;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingReviewDTO {

    @NotNull
    @Min(1)
    private Long bookingId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    @NotBlank
    private String content;
}
