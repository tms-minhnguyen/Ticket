package com.example.ticket.task;

import com.example.ticket.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled task to handle order expiration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpirationTask {

    private final OrderService orderService;

    /**
     * Run every minute to cleanup expired pending orders.
     */
    @Scheduled(fixedRate = 60000)
    public void cleanupExpiredOrders() {
        // log.info("Running order cleanup task..."); 
        // Commented out to avoid spamming logs, OrderService will log actual expiration
        orderService.expireOldOrders();
    }
}
