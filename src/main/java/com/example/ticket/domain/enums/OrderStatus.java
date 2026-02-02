package com.example.ticket.domain.enums;

/**
 * Represents the status of an order in the ticket system.
 */
public enum OrderStatus {
    /**
     * Order created, waiting for payment
     */
    PENDING,

    /**
     * Payment completed successfully
     */
    PAID,

    /**
     * Payment failed or order expired
     */
    FAILED,

    /**
     * Order cancelled by user
     */
    CANCELLED,

    /**
     * Order refunded
     */
    REFUNDED
}
