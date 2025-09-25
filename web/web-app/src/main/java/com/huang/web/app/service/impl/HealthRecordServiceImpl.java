package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.HealthRecord;
import com.huang.web.app.mapper.HealthRecordMapper;
import com.huang.web.app.service.HealthRecordService;
import org.springframework.stereotype.Service;

/**
 * HealthRecord服务实现类
 * @author system
 * @since 2025-01-24
 */
@Service
public class HealthRecordServiceImpl extends ServiceImpl<HealthRecordMapper, HealthRecord> implements HealthRecordService {

}
