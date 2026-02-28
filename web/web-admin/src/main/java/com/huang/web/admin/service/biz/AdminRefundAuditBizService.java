package com.huang.web.admin.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.PaymentRecord;
import com.huang.model.entity.RefundRecord;
import com.huang.web.admin.mapper.OrderInfoMapper;
import com.huang.web.admin.mapper.PaymentRecordMapper;
import com.huang.web.admin.mapper.RefundRecordMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminRefundAuditBizService {

    private final RefundRecordMapper refundRecordMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final PaymentRecordMapper paymentRecordMapper;

    public AdminRefundAuditBizService(RefundRecordMapper refundRecordMapper,
                                      OrderInfoMapper orderInfoMapper,
                                      PaymentRecordMapper paymentRecordMapper) {
        this.refundRecordMapper = refundRecordMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.paymentRecordMapper = paymentRecordMapper;
    }

    public List<Map<String, Object>> listRefundAudits(String refundStatus,
                                                      Long userId,
                                                      String orderNo,
                                                      LocalDateTime startTime,
                                                      LocalDateTime endTime,
                                                      int limit) {
        Map<Long, OrderInfo> orderCache = new HashMap<>();
        Set<Long> filteredOrderIds = null;

        if (userId != null || (orderNo != null && !orderNo.isBlank())) {
            LambdaQueryWrapper<OrderInfo> orderFilter = new LambdaQueryWrapper<>();
            if (userId != null) {
                orderFilter.eq(OrderInfo::getUserId, userId);
            }
            if (orderNo != null && !orderNo.isBlank()) {
                orderFilter.like(OrderInfo::getOrderNo, orderNo.trim());
            }
            List<OrderInfo> filteredOrders = orderInfoMapper.selectList(orderFilter);
            if (filteredOrders.isEmpty()) {
                return List.of();
            }
            orderCache.putAll(filteredOrders.stream().collect(Collectors.toMap(OrderInfo::getId, x -> x)));
            filteredOrderIds = orderCache.keySet();
        }

        LambdaQueryWrapper<RefundRecord> wrapper = new LambdaQueryWrapper<RefundRecord>()
                .orderByDesc(RefundRecord::getId)
                .last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        if (refundStatus != null && !refundStatus.isBlank()) {
            wrapper.eq(RefundRecord::getRefundStatus, refundStatus);
        }
        if (startTime != null) {
            wrapper.ge(RefundRecord::getRefundTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(RefundRecord::getRefundTime, endTime);
        }
        if (filteredOrderIds != null) {
            wrapper.in(RefundRecord::getOrderId, filteredOrderIds);
        }

        List<RefundRecord> refunds = refundRecordMapper.selectList(wrapper);
        if (refunds.isEmpty()) {
            return List.of();
        }

        Set<Long> orderIds = refunds.stream().map(RefundRecord::getOrderId).collect(Collectors.toSet());
        if (orderCache.isEmpty()) {
            List<OrderInfo> orders = orderInfoMapper.selectBatchIds(orderIds);
            orderCache.putAll(orders.stream().collect(Collectors.toMap(OrderInfo::getId, x -> x)));
        }

        Map<Long, PaymentRecord> paymentCache = new HashMap<>();
        List<PaymentRecord> payments = paymentRecordMapper.selectList(
                new LambdaQueryWrapper<PaymentRecord>()
                        .in(PaymentRecord::getOrderId, orderIds)
                        .orderByDesc(PaymentRecord::getId)
        );
        for (PaymentRecord payment : payments) {
            paymentCache.putIfAbsent(payment.getOrderId(), payment);
        }

        List<Map<String, Object>> rows = new ArrayList<>(refunds.size());
        for (RefundRecord refund : refunds) {
            OrderInfo order = orderCache.get(refund.getOrderId());
            PaymentRecord pay = paymentCache.get(refund.getOrderId());
            rows.add(buildRow(refund, order, pay));
        }
        return rows;
    }

    private Map<String, Object> buildRow(RefundRecord refund, OrderInfo order, PaymentRecord pay) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("refund", refund);
        row.put("userId", order == null ? null : order.getUserId());
        row.put("orderNo", order == null ? null : order.getOrderNo());
        row.put("bizType", order == null ? null : order.getBizType());
        row.put("orderStatus", order == null ? null : order.getOrderStatus());
        row.put("payStatus", pay == null ? null : pay.getPayStatus());
        row.put("payNo", pay == null ? null : pay.getPayNo());
        return row;
    }
}
