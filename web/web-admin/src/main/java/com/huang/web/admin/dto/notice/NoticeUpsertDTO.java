package com.huang.web.admin.dto.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NoticeUpsertDTO {

    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private Integer status;

    private LocalDateTime publishTime;
}

