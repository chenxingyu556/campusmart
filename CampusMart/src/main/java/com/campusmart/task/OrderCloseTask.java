package com.campusmart.task;

import com.campusmart.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderCloseTask {

    private static final Logger log = LoggerFactory.getLogger(OrderCloseTask.class);

    private final OrderService orderService;

    public OrderCloseTask(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 60_000)
    public void closeExpired() {
        try {
            int n = orderService.closeExpiredOrders();
            if (n > 0) {
                log.info("超时关单 {} 笔", n);
            }
        } catch (Exception e) {
            log.error("超时关单任务异常", e);
        }
    }
}
