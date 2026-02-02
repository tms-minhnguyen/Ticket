package com.example.ticket.controller;

import com.example.ticket.domain.entity.Payment;
import com.example.ticket.domain.enums.PaymentStatus;
import com.example.ticket.service.OrderService;
import com.example.ticket.service.VNPayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST controller for payment-related operations.
 * Handles VNPay callbacks (return URL and IPN).
 */
@Slf4j
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final VNPayService vnPayService;
    private final OrderService orderService;

    /**
     * VNPay Return URL - Called when user is redirected back from VNPay.
     * This is for user experience (redirect to success/failure page).
     */
    @GetMapping("/vnpay/callback")
    public ResponseEntity<Map<String, Object>> vnpayCallback(@RequestParam Map<String, String> params) {
        log.info("VNPay callback received: {}", params);

        try {
            // Verify signature
            boolean isValid = vnPayService.verifyCallback(params);
            if (!isValid) {
                log.warn("Invalid VNPay signature");
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Invalid signature"));
            }

            // Process the callback
            Payment payment = vnPayService.processCallback(params);
            String orderCode = payment.getOrder().getOrderCode();

            if (payment.getStatus() == PaymentStatus.SUCCESS) {
                // Trigger async order finalization via SQS
                orderService.handlePaymentSuccess(orderCode);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Payment successful",
                        "orderCode", orderCode,
                        "redirectUrl", "/payment/success?orderCode=" + orderCode));
            } else {
                // Handle payment failure
                orderService.handlePaymentFailure(orderCode);

                Long eventId = payment.getOrder().getEvent().getId();

                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "Payment failed",
                        "orderCode", orderCode,
                        "eventId", eventId,
                        "responseCode", params.get("vnp_ResponseCode"),
                        "redirectUrl", "/payment/failed?orderCode=" + orderCode + "&eventId=" + eventId));
            }
        } catch (Exception e) {
            log.error("Error processing VNPay callback", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    /**
     * VNPay IPN URL - Instant Payment Notification (webhook).
     * This is the server-to-server callback for reliable payment confirmation.
     */
    @PostMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN received: {}", params);

        try {
            // Verify signature
            boolean isValid = vnPayService.verifyCallback(params);
            if (!isValid) {
                log.warn("Invalid VNPay IPN signature");
                return ResponseEntity.ok(Map.of(
                        "RspCode", "97",
                        "Message", "Invalid signature"));
            }

            String vnpTxnRef = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");

            // Process the IPN
            Payment payment = vnPayService.processCallback(params);
            String orderCode = payment.getOrder().getOrderCode();

            if ("00".equals(responseCode)) {
                // Payment successful - trigger async processing
                orderService.handlePaymentSuccess(orderCode);

                return ResponseEntity.ok(Map.of(
                        "RspCode", "00",
                        "Message", "Confirm Success"));
            } else {
                // Payment failed
                orderService.handlePaymentFailure(orderCode);

                return ResponseEntity.ok(Map.of(
                        "RspCode", "00",
                        "Message", "Confirm Success"));
            }
        } catch (Exception e) {
            log.error("Error processing VNPay IPN", e);
            return ResponseEntity.ok(Map.of(
                    "RspCode", "99",
                    "Message", "Unknown error"));
        }
    }

    /**
     * Get payment status by order code.
     */
    @GetMapping("/status/{orderCode}")
    public ResponseEntity<Map<String, Object>> getPaymentStatus(@PathVariable String orderCode) {
        try {
            Payment payment = vnPayService.getPaymentByOrderCode(orderCode);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "orderCode", orderCode,
                    "paymentStatus", payment.getStatus().name(),
                    "amount", payment.getAmount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}
