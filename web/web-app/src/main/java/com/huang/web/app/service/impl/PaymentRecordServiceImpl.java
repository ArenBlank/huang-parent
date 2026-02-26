package com.huang.web.app.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.huang.model.entity.PaymentRecord;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.service.PaymentRecordService;
import org.springframework.stereotype.Service;

/**
 * PaymentRecord服务实现类
 * @author system
 * @since 2026-02-25
 */
@Service
public class PaymentRecordServiceImpl extends ServiceImpl<PaymentRecordMapper, PaymentRecord> implements PaymentRecordService {

}
