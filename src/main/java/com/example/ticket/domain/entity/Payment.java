package com.example.ticket.domain.entity;

import com.example.ticket.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a payment transaction for an order.
 */
@Entity
@Table(name = "payments", indexes = {
        @Index(name = "idx_payment_order", columnList = "order_id"),
        @Index(name = "idx_payment_txn_ref", columnList = "vnpay_txn_ref"),
        @Index(name = "idx_payment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "payment_method", nullable = false, length = 20)
    @Builder.Default
    private String paymentMethod = "VNPAY";

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "vnpay_txn_ref", length = 100)
    private String vnpayTxnRef;

    @Column(name = "vnpay_transaction_no", length = 100)
    private String vnpayTransactionNo;

    @Column(name = "vnpay_response_code", length = 10)
    private String vnpayResponseCode;

    @Column(name = "vnpay_bank_code", length = 20)
    private String vnpayBankCode;

    @Column(name = "vnpay_pay_date")
    private LocalDateTime vnpayPayDate;

    @Column(name = "payment_url", length = 1000)
    private String paymentUrl;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    /**
     * Check if payment was successful.
     */
    public boolean isSuccess() {
        return status == PaymentStatus.SUCCESS;
    }
}
