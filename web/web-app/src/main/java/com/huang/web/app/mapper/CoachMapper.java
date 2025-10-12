package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.Coach;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * App端教练Mapper
 * @author system
 * @since 2025-01-25
 */
@Mapper
public interface CoachMapper extends BaseMapper<Coach> {

    /**
     * 根据用户ID查询教练信息
     */
    Coach selectByUserId(@Param("userId") Long userId);
}
