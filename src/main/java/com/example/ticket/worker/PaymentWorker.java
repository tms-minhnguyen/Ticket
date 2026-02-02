package com.example.ticket.worker;

import com.example.ticket.service.OrderService;
import com.example.ticket.service.SqsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

/**
 * Payment Worker - Polls SQS for ORDER_PAID messages and finalizes orders.
 * This is Phase 3 of the flow: Async order finalization.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentWorker {

    private final SqsService sqsService;
    private final OrderService orderService;

    /**
     * Poll SQS queue for payment messages every second.
     */
    @Scheduled(fixedDelay = 1000)
    public void pollPaymentMessages() {
        try {
            List<Message> messages = sqsService.receiveMessages(10).join();

            for (Message message : messages) {
                processMessage(message);
            }
        } catch (Exception e) {
            log.error("Error polling SQS messages", e);
        }
    }

    /**
     * Process a single SQS message.
     */
    private void processMessage(Message message) {
        String orderCode = null;
        try {
            orderCode = sqsService.extractOrderCode(message.body());

            if (orderCode == null) {
                log.warn("Could not extract order code from message: {}", message.body());
                deleteMessage(message);
                return;
            }

            log.info("Processing ORDER_PAID message for order: {}", orderCode);

            // Finalize the order
            orderService.markOrderPaid(orderCode);

            // Delete message after successful processing
            deleteMessage(message);

            log.info("Successfully processed order: {}", orderCode);
        } catch (Exception e) {
            log.error("Error processing message for order {}: {}", orderCode, e.getMessage());
            // Message will become visible again after visibility timeout
        }
    }

    /**
     * Delete message from queue.
     */
    private void deleteMessage(Message message) {
        try {
            sqsService.deleteMessage(message.receiptHandle()).join();
        } catch (Exception e) {
            log.error("Error deleting message: {}", e.getMessage());
        }
    }

    /**
     * Scheduled task to expire old orders.
     * Runs every minute.
     */
    @Scheduled(fixedRate = 60000)
    public void expireOrders() {
        try {
            orderService.expireOldOrders();
        } catch (Exception e) {
            log.error("Error expiring orders", e);
        }
    }
}
