package com.huang.web.app.vo.coach;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * App端教练咨询VO
 * @author system
 * @since 2025-01-25
 */
@Schema(description = "教练咨询VO")
@Data
public class CoachConsultationVO {

    @Schema(description = "咨询ID")
    private Long id;

    @Schema(description = "教练ID")
    private Long coachId;

    @Schema(description = "教练姓名")
    private String coachName;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "咨询类型: online在线 offline线下")
    private String consultationType;

    @Schema(description = "咨询类型描述")
    private String consultationTypeDesc;

    @Schema(description = "咨询时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consultationDate;

    @Schema(description = "咨询时长(分钟)")
    private Integer duration;

    @Schema(description = "咨询主题")
    private String topic;

    @Schema(description = "咨询内容")
    private String content;

    @Schema(description = "教练建议")
    private String coachAdvice;

    @Schema(description = "状态: scheduled已预约 completed已完成 cancelled已取消")
    private String status;

    @Schema(description = "状态描述")
    private String statusDesc;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "是否可以取消")
    private Integer canCancel;
}
