package com.campusmart.listener;

import com.campusmart.config.RabbitMQConfig;
import com.campusmart.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderCloseListener {

    private final OrderService orderService;

    public OrderCloseListener(OrderService orderService) {
        this.orderService = orderService;
    }

    @RabbitListener(queues = RabbitMQConfig.CLOSE_QUEUE)
    public void onOrderClose(String orderIdStr) {
        Long orderId = Long.parseLong(orderIdStr);
        boolean closed = orderService.closeOrder(orderId);
        log.info("延迟关单 orderId={} 结果={}", orderId, closed ? "已关闭" : "跳过(状态已变)");
    }
}
