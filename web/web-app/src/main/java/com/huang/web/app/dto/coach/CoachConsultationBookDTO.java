package com.huang.web.app.dto.coach;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;

/**
 * App端教练咨询预约DTO
 * @author system
 * @since 2025-01-25
 */
@Schema(description = "教练咨询预约DTO")
@Data
public class CoachConsultationBookDTO {

    @Schema(description = "教练ID", required = true)
    @NotNull(message = "教练ID不能为空")
    private Long coachId;

    @Schema(description = "咨询类型: online在线 offline线下", required = true)
    @NotBlank(message = "咨询类型不能为空")
    private String consultationType;

    @Schema(description = "咨询时间", required = true, example = "2025-02-01 10:00:00")
    @NotNull(message = "咨询时间不能为空")
    @Future(message = "咨询时间必须是未来时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consultationDate;

    @Schema(description = "咨询时长(分钟)", required = true, example = "60")
    @NotNull(message = "咨询时长不能为空")
    @Min(value = 30, message = "咨询时长至少30分钟")
    private Integer duration;

    @Schema(description = "咨询主题", required = true)
    @NotBlank(message = "咨询主题不能为空")
    @Size(max = 200, message = "咨询主题不能超过200字")
    private String topic;

    @Schema(description = "咨询内容")
    @Size(max = 1000, message = "咨询内容不能超过1000字")
    private String content;
}
