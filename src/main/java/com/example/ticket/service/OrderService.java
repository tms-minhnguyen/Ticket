package com.example.ticket.service;

import com.example.ticket.domain.entity.Event;
import com.example.ticket.domain.entity.Order;
import com.example.ticket.domain.entity.Payment;
import com.example.ticket.domain.enums.OrderStatus;
import com.example.ticket.domain.enums.PaymentStatus;
import com.example.ticket.domain.repository.EventRepository;
import com.example.ticket.domain.repository.OrderRepository;
import com.example.ticket.dto.request.CreateOrderRequest;
import com.example.ticket.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing orders.
 * Implements the ticket holding and order creation flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final EventRepository eventRepository;
    private final com.example.ticket.domain.repository.UserRepository userRepository; // Add this
    private final InventoryService inventoryService;
    private final VNPayService vnPayService;
    private final SqsService sqsService;

    @Value("${ticket.hold-ttl-minutes:15}")
    private int holdTtlMinutes;

    /**
     * Create a new order with ticket hold.
     * Phase 1 of the flow: Hold tickets atomically in Redis.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String ipAddress, Long userId) { // Add userId param
        // 1. Get event
        Event event = eventRepository.findById(request.getEventId())
                .orElseThrow(() -> new RuntimeException("Event not found: " + request.getEventId()));

        if (!event.isOnSale()) {
            throw new RuntimeException("Event is not on sale");
        }

        // 1b. Check Flash Sale Constraints
        // Check sale time
        if (event.getSaleStartTime() != null && LocalDateTime.now().isBefore(event.getSaleStartTime())) {
            throw new RuntimeException("Sale has not started yet");
        }
        if (event.getSaleEndTime() != null && LocalDateTime.now().isAfter(event.getSaleEndTime())) {
            throw new RuntimeException("Sale has ended");
        }

        // Check max tickets per user
        if (event.getMaxTicketsPerUser() != null) {
            int currentTickets = orderRepository.countTicketsByUserAndEvent(userId, event.getId());
            if (currentTickets + request.getQuantity() > event.getMaxTicketsPerUser()) {
                throw new RuntimeException(
                        "Exceeds maximum tickets allowed per user (" + event.getMaxTicketsPerUser() + ")");
            }
        }

        // 2. Hold tickets atomically in Redis
        boolean held = inventoryService.holdTickets(event.getId(), request.getQuantity());
        if (!held) {
            throw new RuntimeException("Not enough tickets available");
        }

        // 2b. Get User
        com.example.ticket.domain.entity.User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            // 3. Create order with PENDING status
            String orderCode = generateOrderCode();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expiredAt = now.plusMinutes(holdTtlMinutes);

            Order order = Order.builder()
                    .orderCode(orderCode)
                    .user(user) // Link user
                    .event(event)
                    .quantity(request.getQuantity())
                    .totalAmount(event.getBasePrice().multiply(java.math.BigDecimal.valueOf(request.getQuantity())))
                    .status(OrderStatus.PENDING)
                    .expiredAt(expiredAt)
                    .customerName(request.getCustomerName())
                    .customerEmail(request.getCustomerEmail())
                    .customerPhone(request.getCustomerPhone())
                    .build();

            Order savedOrder = orderRepository.save(order);

            // 4. Create payment record and get VNPay URL
            Payment payment = vnPayService.createPayment(savedOrder, ipAddress);

            // 5. Set hold key with TTL in Redis for auto-expiration
            inventoryService.setOrderHold(orderCode, event.getId(), request.getQuantity(),
                    Duration.ofMinutes(holdTtlMinutes));

            // 6. Update database inventory (for consistency)
            eventRepository.decrementAvailableTickets(event.getId(), request.getQuantity());

            log.info("Created order {} for {} tickets of event {}",
                    orderCode, request.getQuantity(), event.getName());

            return toResponse(savedOrder, payment);
        } catch (Exception e) {
            // Rollback Redis hold on any error
            inventoryService.releaseTickets(event.getId(), request.getQuantity());
            throw e;
        }
    }

    /**
     * Get order by order code.
     */
    @Transactional(readOnly = true)
    public OrderResponse getOrderByCode(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

        Payment payment = order.getPayment();
        return toResponse(order, payment);
    }

    /**
     * Get orders by user (for order history).
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Long userId, Pageable pageable) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(order -> toResponse(order, order.getPayment()));
    }

    /**
     * Mark order as paid.
     * Called by Payment Worker after processing SQS message.
     */
    @Transactional
    public void markOrderPaid(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

        if (order.getStatus() == OrderStatus.PAID) {
            log.warn("Order {} is already paid", orderCode);
            return;
        }

        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(LocalDateTime.now());
        orderRepository.save(order);

        // Remove hold key from Redis (tickets are now sold)
        inventoryService.removeOrderHold(orderCode);

        log.info("Order {} marked as PAID", orderCode);
    }

    /**
     * Handle payment success callback.
     * Sends message to SQS for async processing.
     */
    @Transactional
    public void handlePaymentSuccess(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not in PENDING status", orderCode);
            return;
        }

        // Push message to SQS for async order finalization
        sqsService.sendOrderPaidMessage(orderCode);
        log.info("Sent ORDER_PAID message to SQS for order {}", orderCode);
    }

    /**
     * Handle payment failure.
     * Release held tickets back to inventory.
     */
    @Transactional
    public void handlePaymentFailure(String orderCode) {
        Order order = orderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderCode));

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Order {} is not in PENDING status", orderCode);
            return;
        }

        // Update order status
        order.setStatus(OrderStatus.FAILED);
        orderRepository.save(order);

        // Release tickets back to inventory
        inventoryService.releaseTickets(order.getEvent().getId(), order.getQuantity());
        inventoryService.removeOrderHold(orderCode);

        // Update database inventory
        int updatedRows = eventRepository.incrementAvailableTickets(order.getEvent().getId(), order.getQuantity());
        log.info("Order {} marked as FAILED, tickets released. Rows updated: {}", orderCode, updatedRows);
    }

    /**
     * Expire pending orders that have passed their TTL.
     * Called by scheduled task.
     */
    @Transactional
    public void expireOldOrders() {
        var expiredOrders = orderRepository.findExpiredOrders(OrderStatus.PENDING, LocalDateTime.now());

        for (Order order : expiredOrders) {
            try {
                handlePaymentFailure(order.getOrderCode());
                log.info("Expired order: {}", order.getOrderCode());
            } catch (Exception e) {
                log.error("Error expiring order {}", order.getOrderCode(), e);
            }
        }
    }

    private String generateOrderCode() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse toResponse(Order order, Payment payment) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderCode(order.getOrderCode())
                .eventId(order.getEvent().getId())
                .eventName(order.getEvent().getName())
                .quantity(order.getQuantity())
                .totalAmount(order.getTotalAmount())
                .status(order.getStatus())
                .createdAt(order.getCreatedAt())
                .expiredAt(order.getExpiredAt())
                .paidAt(order.getPaidAt())
                .customerName(order.getCustomerName())
                .customerEmail(order.getCustomerEmail())
                .customerPhone(order.getCustomerPhone())
                .paymentUrl(payment != null ? payment.getPaymentUrl() : null)
                .paymentStatus(payment != null ? payment.getStatus().name() : null)
                .build();
    }
}
