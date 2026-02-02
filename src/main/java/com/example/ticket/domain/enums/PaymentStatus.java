package com.example.ticket.domain.enums;

/**
 * Represents the status of a payment transaction.
 */
public enum PaymentStatus {
    /**
     * Payment initiated, waiting for user action
     */
    PENDING,

    /**
     * Payment completed successfully
     */
    SUCCESS,

    /**
     * Payment failed
     */
    FAILED,

    /**
     * Payment refunded
     */
    REFUNDED,

    /**
     * Payment expired (timeout)
     */
    EXPIRED
}
