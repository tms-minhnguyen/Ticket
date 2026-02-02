package com.example.ticket.service;

import com.example.ticket.config.VNPayConfig;
import com.example.ticket.domain.entity.Order;
import com.example.ticket.domain.entity.Payment;
import com.example.ticket.domain.enums.PaymentStatus;
import com.example.ticket.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service for VNPay payment integration.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final PaymentRepository paymentRepository;

    private static final DateTimeFormatter VN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Create VNPay payment URL and save payment record.
     */
    @Transactional
    public Payment createPayment(Order order, String ipAddress) {
        String vnpTxnRef = generateTxnRef(order.getOrderCode());
        String paymentUrl = buildPaymentUrl(order, vnpTxnRef, ipAddress);

        Payment payment = Payment.builder()
                .order(order)
                .amount(order.getTotalAmount())
                .paymentMethod("VNPAY")
                .status(PaymentStatus.PENDING)
                .vnpayTxnRef(vnpTxnRef)
                .paymentUrl(paymentUrl)
                .ipAddress(ipAddress)
                .build();

        Payment saved = paymentRepository.save(payment);
        log.info("Created payment for order {}: {}", order.getOrderCode(), vnpTxnRef);
        return saved;
    }

    /**
     * Build VNPay payment URL with all required parameters.
     */
    public String buildPaymentUrl(Order order, String vnpTxnRef, String ipAddress) {
        Map<String, String> vnpParams = new TreeMap<>();

        vnpParams.put("vnp_Version", vnPayConfig.getVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(order.getTotalAmount().longValue() * 100));
        vnpParams.put("vnp_CurrCode", vnPayConfig.getCurrCode());
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Payment for order " + order.getOrderCode());
        vnpParams.put("vnp_OrderType", vnPayConfig.getOrderType());
        vnpParams.put("vnp_Locale", vnPayConfig.getLocale());
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddress);
        vnpParams.put("vnp_CreateDate", getCurrentDateString());
        vnpParams.put("vnp_ExpireDate", getExpireDateString(15)); // 15 minutes

        // Build query string
        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (Map.Entry<String, String> entry : vnpParams.entrySet()) {
            if (query.length() > 0) {
                query.append("&");
                hashData.append("&");
            }
            query.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            hashData.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }

        // Create secure hash
        String vnpSecureHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        query.append("&vnp_SecureHash=").append(vnpSecureHash);

        String paymentUrl = vnPayConfig.getPaymentUrl() + "?" + query;
        log.debug("Generated payment URL for order {}", order.getOrderCode());
        return paymentUrl;
    }

    /**
     * Verify VNPay callback signature.
     */
    public boolean verifyCallback(Map<String, String> params) {
        String vnpSecureHash = params.get("vnp_SecureHash");
        if (vnpSecureHash == null) {
            return false;
        }

        Map<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getKey().startsWith("vnp_") && !"vnp_SecureHash".equals(entry.getKey())
                    && !"vnp_SecureHashType".equals(entry.getKey())) {
                sortedParams.put(entry.getKey(), entry.getValue());
            }
        }

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> iterator = sortedParams.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            String key = entry.getKey();
            String value = entry.getValue();

            if (value != null && !value.isEmpty()) {
                hashData.append(key);
                hashData.append("=");
                hashData.append(URLEncoder.encode(value, StandardCharsets.UTF_8));

                if (iterator.hasNext()) {
                    hashData.append("&");
                }
            }
        }

        // Remove trailing & if strictly needed or manage inside loop carefully (above
        // loop adds & only if hasNext but we might skip empty values which breaks
        // logic)
        // Safer approach:
        hashData = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                if (hashData.length() > 0) {
                    hashData.append("&");
                }
                hashData.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            }
        }

        String calculatedHash = hmacSHA512(vnPayConfig.getHashSecret(), hashData.toString());
        return calculatedHash.equalsIgnoreCase(vnpSecureHash);
    }

    /**
     * Process VNPay callback and update payment status.
     */
    @Transactional
    public Payment processCallback(Map<String, String> params) {
        String vnpTxnRef = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");
        String bankCode = params.get("vnp_BankCode");
        String payDateStr = params.get("vnp_PayDate");

        Payment payment = paymentRepository.findByVnpayTxnRef(vnpTxnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found: " + vnpTxnRef));

        payment.setVnpayResponseCode(responseCode);
        payment.setVnpayTransactionNo(transactionNo);
        payment.setVnpayBankCode(bankCode);

        if (payDateStr != null && !payDateStr.isEmpty()) {
            payment.setVnpayPayDate(LocalDateTime.parse(payDateStr, VN_DATE_FORMAT));
        }

        // Check if payment was successful (response code 00)
        if ("00".equals(responseCode)) {
            payment.setStatus(PaymentStatus.SUCCESS);
            log.info("Payment successful for txnRef: {}", vnpTxnRef);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            log.warn("Payment failed for txnRef: {} with code: {}", vnpTxnRef, responseCode);
        }

        Payment saved = paymentRepository.save(payment);

        // Eagerly fetch/init order to avoid LazyInitException in Controller
        // Or simply relying on current session. Accessing a property triggers init.
        if (saved.getOrder() != null) {
            saved.getOrder().getOrderCode(); // Trigger init
        }

        return saved;
    }

    /**
     * Get payment by order code.
     */
    @Transactional(readOnly = true)
    public Payment getPaymentByOrderCode(String orderCode) {
        return paymentRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Payment not found for order: " + orderCode));
    }

    private String generateTxnRef(String orderCode) {
        return orderCode + "_" + System.currentTimeMillis();
    }

    private String getCurrentDateString() {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh")).format(VN_DATE_FORMAT);
    }

    private String getExpireDateString(int minutes) {
        return LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .plusMinutes(minutes)
                .format(VN_DATE_FORMAT);
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmacSha512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmacSha512.init(secretKey);
            byte[] hash = hmacSha512.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating HMAC-SHA512", e);
        }
    }
}
