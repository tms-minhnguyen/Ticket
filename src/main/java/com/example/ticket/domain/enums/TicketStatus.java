package com.example.ticket.domain.enums;

/**
 * Represents the status of a ticket.
 */
public enum TicketStatus {
    /**
     * Ticket is available for purchase
     */
    AVAILABLE,

    /**
     * Ticket is temporarily held (reserved) during checkout
     */
    HELD,

    /**
     * Ticket has been sold and assigned to a user
     */
    SOLD,

    /**
     * Ticket was cancelled/refunded
     */
    CANCELLED
}
