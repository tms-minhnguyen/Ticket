package com.example.ticket.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VNPay configuration properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "vnpay")
public class VNPayConfig {

    /**
     * VNPay terminal code (provided by VNPay).
     */
    private String tmnCode = "DEMO";

    /**
     * VNPay hash secret key (provided by VNPay).
     */
    private String hashSecret = "DEMOSECRETKEY";

    /**
     * VNPay payment gateway URL.
     */
    private String paymentUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";

    /**
     * Return URL after payment completes.
     */
    private String returnUrl = "http://localhost:5173/payment/callback";

    /**
     * IPN URL for VNPay webhook callbacks.
     */
    private String ipnUrl = "http://localhost:8080/api/payments/vnpay/ipn";

    /**
     * API URL for query transactions.
     */
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    /**
     * Version of VNPay API.
     */
    private String version = "2.1.0";

    /**
     * Command for payment.
     */
    private String command = "pay";

    /**
     * Order type.
     */
    private String orderType = "other";

    /**
     * Currency code (VND).
     */
    private String currCode = "VND";

    /**
     * Locale for payment page.
     */
    private String locale = "vn";
}
