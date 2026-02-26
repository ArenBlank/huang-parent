package com.huang.web.admin.custom.schedule;

import com.huang.web.admin.service.biz.AdminOpsBizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingTimeoutSchedule {

    private final AdminOpsBizService adminOpsBizService;

    public BookingTimeoutSchedule(AdminOpsBizService adminOpsBizService) {
        this.adminOpsBizService = adminOpsBizService;
    }

    @Scheduled(fixedDelay = 60000)
    public void closeTimeoutUnpaidBookings() {
        // 工程亮点：定时补偿任务兜底，处理“创建成功但长时间未支付”的悬挂订单。
        int closed = adminOpsBizService.closeTimeoutUnpaidBookings(30);
        if (closed > 0) {
            log.info("auto-close unpaid bookings count={}", closed);
        }
    }
}
