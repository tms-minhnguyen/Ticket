package com.example.ticket.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for VNPay payment creation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentUrlResponse {

    private String orderCode;
    private String paymentUrl;
    private String message;
}
