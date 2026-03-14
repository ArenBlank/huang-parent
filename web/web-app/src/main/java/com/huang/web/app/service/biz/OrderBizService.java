package com.huang.web.app.service.biz;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huang.model.entity.OrderInfo;
import com.huang.model.entity.OrderItem;
import com.huang.model.entity.PaymentRecord;
import com.huang.model.entity.RefundRecord;
import com.huang.web.app.mapper.OrderInfoMapper;
import com.huang.web.app.mapper.OrderItemMapper;
import com.huang.web.app.mapper.PaymentRecordMapper;
import com.huang.web.app.mapper.RefundRecordMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

@Service
public class OrderBizService {

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final RefundRecordMapper refundRecordMapper;

    public OrderBizService(OrderInfoMapper orderInfoMapper,
                           OrderItemMapper orderItemMapper,
                           PaymentRecordMapper paymentRecordMapper,
                           RefundRecordMapper refundRecordMapper) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentRecordMapper = paymentRecordMapper;
        this.refundRecordMapper = refundRecordMapper;
    }

    public Map<String, Object> detail(Long userId, Long orderId) {
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || userId == null || !userId.equals(order.getUserId())) {
            return null;
        }
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId)
        );
        PaymentRecord payment = paymentRecordMapper.selectOne(
                new LambdaQueryWrapper<PaymentRecord>()
                        .eq(PaymentRecord::getOrderId, orderId)
                        .orderByDesc(PaymentRecord::getId)
                        .last("LIMIT 1")
        );
        RefundRecord refund = refundRecordMapper.selectOne(
                new LambdaQueryWrapper<RefundRecord>()
                        .eq(RefundRecord::getOrderId, orderId)
                        .orderByDesc(RefundRecord::getId)
                        .last("LIMIT 1")
        );
        BigDecimal paidAmount = payment != null && payment.getPayAmount() != null
                ? payment.getPayAmount()
                : BigDecimal.ZERO;
        BigDecimal refundAmount = refund != null && refund.getRefundAmount() != null
                ? refund.getRefundAmount()
                : BigDecimal.ZERO;
        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("payment", payment);
        result.put("refund", refund);
        result.put("paidAmount", paidAmount);
        result.put("refundAmount", refundAmount);
        result.put("netPaid", paidAmount.subtract(refundAmount));
        result.put("payStatus", payment != null ? payment.getPayStatus() : order.getPayStatus());
        result.put("payChannel", payment != null ? payment.getPayChannel() : null);
        result.put("payTime", payment != null ? payment.getPayTime() : null);
        result.put("refundStatus", refund != null ? refund.getRefundStatus() : null);
        result.put("refundTime", refund != null ? refund.getRefundTime() : null);
        result.put("refundReason", refund != null ? refund.getReason() : null);
        Map<String, Object> financeSummary = new HashMap<>();
        financeSummary.put("paidAmount", paidAmount);
        financeSummary.put("refundAmount", refundAmount);
        financeSummary.put("netPaid", paidAmount.subtract(refundAmount));
        financeSummary.put("payStatus", payment != null ? payment.getPayStatus() : order.getPayStatus());
        financeSummary.put("refundStatus", refund != null ? refund.getRefundStatus() : null);
        financeSummary.put("statusText", buildFinanceStatusText(payment, refund, order));
        financeSummary.put("statusHint", buildFinanceStatusHint(payment, refund, order));
        result.put("financeSummary", financeSummary);
        return result;
    }

    private String buildFinanceStatusText(PaymentRecord payment, RefundRecord refund, OrderInfo order) {
        String refundStatus = refund == null ? null : refund.getRefundStatus();
        if ("REFUNDED".equalsIgnoreCase(refundStatus)) {
            return "refunded";
        }
        String payStatus = payment == null ? null : payment.getPayStatus();
        if ("PAID".equalsIgnoreCase(payStatus)) {
            return "paid";
        }
        if ("UNPAID".equalsIgnoreCase(payStatus)) {
            return "unpaid";
        }
        String orderStatus = order == null ? null : order.getOrderStatus();
        if ("CLOSED".equalsIgnoreCase(orderStatus)) {
            return "closed";
        }
        return "unknown";
    }

    private String buildFinanceStatusHint(PaymentRecord payment, RefundRecord refund, OrderInfo order) {
        String refundStatus = refund == null ? null : refund.getRefundStatus();
        if ("REFUNDED".equalsIgnoreCase(refundStatus)) {
            return "order_refunded";
        }
        String payStatus = payment == null ? null : payment.getPayStatus();
        if ("PAID".equalsIgnoreCase(payStatus)) {
            return "order_paid";
        }
        if ("UNPAID".equalsIgnoreCase(payStatus)) {
            String orderStatus = order == null ? null : order.getOrderStatus();
            if ("CLOSED".equalsIgnoreCase(orderStatus)) {
                return "order_closed";
            }
            return "order_unpaid";
        }
        return "order_unknown";
    }
}
