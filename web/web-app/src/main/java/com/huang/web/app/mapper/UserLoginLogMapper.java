package com.huang.web.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.huang.model.entity.UserLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * App端用户登录日志 Mapper
 */
@Mapper
public interface UserLoginLogMapper extends BaseMapper<UserLoginLog> {
}
