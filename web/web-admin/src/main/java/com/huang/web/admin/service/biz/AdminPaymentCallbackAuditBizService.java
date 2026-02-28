package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentCallbackLog;
import com.huang.web.admin.mapper.OrderInfoMapper;
import com.huang.web.admin.mapper.PaymentCallbackLogMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminPaymentCallbackAuditBizService {

    private final PaymentCallbackLogMapper paymentCallbackLogMapper;
    private final OrderInfoMapper orderInfoMapper;

    public AdminPaymentCallbackAuditBizService(PaymentCallbackLogMapper paymentCallbackLogMapper,
                                               OrderInfoMapper orderInfoMapper) {
        this.paymentCallbackLogMapper = paymentCallbackLogMapper;
        this.orderInfoMapper = orderInfoMapper;
    }

    public List<Map<String, Object>> list(String payNo,
                                          String tradeNo,
                                          String processResult,
                                          LocalDateTime startTime,
                                          LocalDateTime endTime,
                                          int limit) {
        LambdaQueryWrapper<PaymentCallbackLog> wrapper = new LambdaQueryWrapper<PaymentCallbackLog>()
                .orderByDesc(PaymentCallbackLog::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        if (payNo != null && !payNo.isBlank()) {
            wrapper.like(PaymentCallbackLog::getPayNo, payNo.trim());
        }
        if (tradeNo != null && !tradeNo.isBlank()) {
            wrapper.like(PaymentCallbackLog::getChannelTradeNo, tradeNo.trim());
        }
        if (processResult != null && !processResult.isBlank()) {
            wrapper.eq(PaymentCallbackLog::getProcessResult, processResult.trim());
        }
        if (startTime != null) {
            wrapper.ge(PaymentCallbackLog::getNotifiedAt, startTime);
        }
        if (endTime != null) {
            wrapper.le(PaymentCallbackLog::getNotifiedAt, endTime);
        }

        List<PaymentCallbackLog> logs = paymentCallbackLogMapper.selectList(wrapper);
        if (logs.isEmpty()) {
            return List.of();
        }

        Set<Long> orderIds = logs.stream()
                .map(PaymentCallbackLog::getOrderId)
                .filter(x -> x != null && x > 0)
                .collect(Collectors.toSet());
        Map<Long, OrderInfo> orders = new HashMap<>();
        if (!orderIds.isEmpty()) {
            orders.putAll(orderInfoMapper.selectBatchIds(orderIds).stream()
                    .collect(Collectors.toMap(OrderInfo::getId, x -> x)));
        }

        return logs.stream().map(log -> {
            OrderInfo order = log.getOrderId() == null ? null : orders.get(log.getOrderId());
            Map<String, Object> row = new HashMap<>();
            row.put("callbackLog", log);
            row.put("orderNo", order == null ? null : order.getOrderNo());
            row.put("userId", order == null ? null : order.getUserId());
            row.put("bizType", order == null ? null : order.getBizType());
            return row;
        }).collect(Collectors.toList());
    }
}

