package com.huang.web.admin.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "角色课程分类范围批量更新请求")
@Data
public class RoleCourseCategoryScopeBatchUpdateDTO {

    @Schema(description = "更新项列表", required = true)
    @NotNull(message = "更新项不能为空")
    private List<RoleCourseCategoryScopeItemDTO> items;
}
