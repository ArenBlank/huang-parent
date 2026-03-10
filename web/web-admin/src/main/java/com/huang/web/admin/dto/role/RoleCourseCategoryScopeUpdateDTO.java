package com.huang.web.admin.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "角色课程分类范围更新请求")
@Data
public class RoleCourseCategoryScopeUpdateDTO {

    @Schema(description = "课程分类ID列表", required = true)
    @NotNull(message = "课程分类范围不能为空")
    private List<Long> categoryIds;
}
