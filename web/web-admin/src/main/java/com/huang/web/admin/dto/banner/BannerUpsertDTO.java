package com.huang.web.admin.dto.banner;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BannerUpsertDTO {

    @NotBlank
    private String title;

    @NotBlank
    private String imageUrl;

    private String linkUrl;

    @NotNull
    @Min(0)
    private Integer sort;

    @NotNull
    private Integer status;
}

