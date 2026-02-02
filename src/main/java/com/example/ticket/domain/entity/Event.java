package com.example.ticket.domain.entity;

import com.example.ticket.domain.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents an event for which tickets can be sold.
 */
@Entity
@Table(name = "events", indexes = {
        @Index(name = "idx_event_status", columnList = "status"),
        @Index(name = "idx_event_date", columnList = "event_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 255)
    private String venue;

    @Column(length = 500)
    private String address;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "total_tickets", nullable = false)
    private Integer totalTickets;

    @Column(name = "available_tickets", nullable = false)
    private Integer availableTickets;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EventStatus status = EventStatus.DRAFT;

    @Column(name = "max_tickets_per_user")
    private Integer maxTicketsPerUser;

    @Column(name = "sale_start_time")
    private LocalDateTime saleStartTime;

    @Column(name = "sale_end_time")
    private LocalDateTime saleEndTime;

    // @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval =
    // true)
    // @Builder.Default
    // private List<Ticket> tickets = new ArrayList<>();

    /**
     * Check if the event is currently on sale.
     */
    /**
     * Check if the event is currently on sale.
     */
    public boolean isOnSale() {
        LocalDateTime now = LocalDateTime.now();
        boolean isTimeValid = true;

        if (saleStartTime != null && now.isBefore(saleStartTime)) {
            isTimeValid = false;
        }
        if (saleEndTime != null && now.isAfter(saleEndTime)) {
            isTimeValid = false;
        }

        return status == EventStatus.ON_SALE &&
                availableTickets > 0 &&
                eventDate.isAfter(now) &&
                isTimeValid;
    }
}
