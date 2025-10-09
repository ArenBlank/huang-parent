package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huang.model.entity.CoachResignationApply;
import com.huang.web.admin.dto.coach.CoachResignationQueryDTO;
import com.huang.web.admin.vo.coach.CoachResignationDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教练离职申请Mapper
 * @author system
 * @since 2025-01-24
 */
@Mapper
public interface CoachResignationApplyMapper extends BaseMapper<CoachResignationApply> {

    /**
     * 分页查询教练离职申请列表
     */
    IPage<CoachResignationDetailVO> selectResignationPage(
            Page<CoachResignationDetailVO> page,
            @Param("query") CoachResignationQueryDTO queryDTO
    );

    /**
     * 根据ID查询教练离职申请详情
     */
    CoachResignationDetailVO selectResignationDetail(@Param("id") Long id);
}