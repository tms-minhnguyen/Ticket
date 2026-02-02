package com.example.ticket.dto.response;

import com.example.ticket.domain.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for order data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {

    private Long id;
    private String orderCode;
    private Long eventId;
    private String eventName;
    private Integer quantity;
    private BigDecimal totalAmount;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime expiredAt;
    private LocalDateTime paidAt;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Payment info
    private String paymentUrl;
    private String paymentStatus;
}
