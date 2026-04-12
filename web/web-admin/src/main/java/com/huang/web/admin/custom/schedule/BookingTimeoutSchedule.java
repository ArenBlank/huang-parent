package com.huang.web.admin.custom.schedule;

import com.huang.common.constant.TaskRunConstant;
import com.huang.common.guard.DistributedTaskLock;
import com.huang.web.admin.service.biz.AdminTaskRunBizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BookingTimeoutSchedule {

    private final AdminTaskRunBizService adminTaskRunBizService;

    public BookingTimeoutSchedule(AdminTaskRunBizService adminTaskRunBizService) {
        this.adminTaskRunBizService = adminTaskRunBizService;
    }

    @Scheduled(fixedDelay = 60000)
    @DistributedTaskLock(taskCode = TaskRunConstant.TASK_BOOKING_TIMEOUT_CLOSE, ttlSec = 180)
    public void closeTimeoutUnpaidBookings() {
        // 工程亮点：定时补偿任务兜底，处理“创建成功但长时间未支付”的悬挂订单。
        int closed = adminTaskRunBizService.runScheduledBookingTimeoutClose(30);
        if (closed > 0) {
            log.info("auto-close unpaid bookings count={}", closed);
        }
    }
}
