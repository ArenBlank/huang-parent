package com.huang.web.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.TempFileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 临时文件记录 Mapper
 */
@Mapper
public interface TempFileRecordMapper extends BaseMapper<TempFileRecord> {

    /**
     * 查询过期的临时文件
     */
    @Select("SELECT * FROM temp_file_record " +
            "WHERE status = 'temp' " +
            "AND expire_time < #{now}")
    List<TempFileRecord> selectExpiredFiles(@Param("now") LocalDateTime now);
}
