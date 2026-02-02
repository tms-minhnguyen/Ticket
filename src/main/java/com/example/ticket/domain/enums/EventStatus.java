package com.example.ticket.domain.enums;

/**
 * Represents the status of an event.
 */
public enum EventStatus {
    /**
     * Event is in draft mode, not visible to users
     */
    DRAFT,

    /**
     * Event is published and tickets are on sale
     */
    ON_SALE,

    /**
     * All tickets are sold out
     */
    SOLD_OUT,

    /**
     * Event has ended
     */
    COMPLETED,

    /**
     * Event was cancelled
     */
    CANCELLED
}
