package com.huang.web.app.custom.schedule;

import com.huang.web.app.service.biz.PaymentCompensationBizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "payment.compensate.enabled", havingValue = "true", matchIfMissing = true)
public class PaymentCompensationTask {

    private static final Logger log = LoggerFactory.getLogger(PaymentCompensationTask.class);

    private final PaymentCompensationBizService paymentCompensationBizService;

    @Value("${payment.compensate.batch:50}")
    private int batchSize;

    public PaymentCompensationTask(PaymentCompensationBizService paymentCompensationBizService) {
        this.paymentCompensationBizService = paymentCompensationBizService;
    }

    @Scheduled(fixedDelayString = "${payment.compensate.delay-ms:120000}")
    public void compensatePaidOrders() {
        int fixed = paymentCompensationBizService.repairPaidOrders(batchSize);
        if (fixed > 0) {
            log.info("PAY_COMPENSATE_DONE fixed={}", fixed);
        }
    }
}
