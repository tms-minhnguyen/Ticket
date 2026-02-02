package com.example.ticket.dto.response;

import com.example.ticket.domain.enums.EventStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for event data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {

    private Long id;
    private String name;
    private String description;
    private String venue;
    private String address;
    private LocalDateTime eventDate;
    private LocalDateTime endDate;
    private BigDecimal basePrice;
    private Integer totalTickets;
    private Integer availableTickets;
    private String imageUrl;
    private EventStatus status;
    private boolean onSale;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
}
