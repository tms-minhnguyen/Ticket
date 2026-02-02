package com.example.ticket.domain.entity;

import com.example.ticket.domain.enums.TicketStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Represents an individual ticket for an event.
 */
// @Entity
// @Table(name = "event_tickets", indexes = {
// @Index(name = "idx_ticket_status", columnList = "status")
// })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "event_id", nullable = false)
    // private Event event;

    @Column(name = "seat_number", length = 50)
    private String seatNumber;

    @Column(length = 50)
    private String zone;

    @Column(name = "row_number", length = 20)
    private String rowNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TicketStatus status = TicketStatus.AVAILABLE;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "order_id")
    // private Order order;

    // @ManyToOne(fetch = FetchType.LAZY)
    // @JoinColumn(name = "user_id")
    // private User owner;

    @Column(name = "ticket_code", unique = true, length = 50)
    private String ticketCode;

    /**
     * Check if ticket is available for purchase.
     */
    public boolean isAvailable() {
        return status == TicketStatus.AVAILABLE;
    }
}
