package com.example.ticket.service;

import com.example.ticket.domain.entity.Event;
import com.example.ticket.domain.enums.EventStatus;
import com.example.ticket.domain.repository.EventRepository;
import com.example.ticket.dto.response.EventResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for managing events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final InventoryService inventoryService;

    /**
     * Get all upcoming events that are on sale.
     */
    @Transactional(readOnly = true)
    public List<EventResponse> getUpcomingEvents() {
        List<Event> events = eventRepository.findUpcomingEventsByStatus(
                EventStatus.ON_SALE, LocalDateTime.now());
        return events.stream().map(this::toResponse).toList();
    }

    /**
     * Get events with pagination.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(Pageable pageable) {
        Page<Event> events = eventRepository.findByStatusAndEventDateAfterOrderByEventDateAsc(
                EventStatus.ON_SALE, LocalDateTime.now(), pageable);
        return events.map(this::toResponse);
    }

    /**
     * Search events by keyword.
     */
    @Transactional(readOnly = true)
    public Page<EventResponse> searchEvents(String keyword, Pageable pageable) {
        Page<Event> events = eventRepository.searchEvents(
                EventStatus.ON_SALE, LocalDateTime.now(), keyword, pageable);
        return events.map(this::toResponse);
    }

    /**
     * Get event by ID.
     */
    @Transactional(readOnly = true)
    public EventResponse getEventById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));

        // Get real-time inventory from Redis
        int redisInventory = inventoryService.getAvailableInventory(id);
        EventResponse response = toResponse(event);
        response.setAvailableTickets(redisInventory > 0 ? redisInventory : event.getAvailableTickets());

        return response;
    }

    /**
     * Create a new event (Admin).
     */
    @Transactional
    public EventResponse createEvent(Event event) {
        event.setStatus(EventStatus.DRAFT);
        event.setAvailableTickets(event.getTotalTickets());
        Event saved = eventRepository.save(event);

        // Initialize Redis inventory
        inventoryService.initializeInventory(saved.getId(), saved.getTotalTickets());

        log.info("Created event: {} with {} tickets", saved.getName(), saved.getTotalTickets());
        return toResponse(saved);
    }

    /**
     * Publish event (make it available for sale).
     */
    @Transactional
    public EventResponse publishEvent(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Event not found: " + id));
        event.setStatus(EventStatus.ON_SALE);
        Event saved = eventRepository.save(event);

        // Ensure Redis inventory is initialized
        inventoryService.initializeInventory(id, event.getAvailableTickets());

        log.info("Published event: {}", saved.getName());
        return toResponse(saved);
    }

    /**
     * Initialize sample events for testing.
     */
    @PostConstruct
    @Transactional
    public void initSampleEvents() {
        if (eventRepository.count() == 0) {
            log.info("Initializing sample events...");

            Event event1 = Event.builder()
                    .name("Rock Concert 2026")
                    .description("Amazing rock concert featuring top bands!")
                    .venue("National Stadium")
                    .address("123 Main Street, Ho Chi Minh City")
                    .eventDate(LocalDateTime.now().plusDays(30))
                    .endDate(LocalDateTime.now().plusDays(30).plusHours(4))
                    .basePrice(new BigDecimal("500000"))
                    .totalTickets(1000)
                    .availableTickets(1000)
                    .imageUrl("https://images.unsplash.com/photo-1540039155733-5bb30b53aa14?w=800")
                    .status(EventStatus.ON_SALE)
                    .build();

            Event event2 = Event.builder()
                    .name("Jazz Night")
                    .description("Smooth jazz under the stars")
                    .venue("Opera House")
                    .address("456 Culture Avenue, Hanoi")
                    .eventDate(LocalDateTime.now().plusDays(14))
                    .endDate(LocalDateTime.now().plusDays(14).plusHours(3))
                    .basePrice(new BigDecimal("350000"))
                    .totalTickets(500)
                    .availableTickets(500)
                    .imageUrl("https://images.unsplash.com/photo-1415201364774-f6f0bb35f28f?w=800")
                    .status(EventStatus.ON_SALE)
                    .build();

            Event event3 = Event.builder()
                    .name("EDM Festival")
                    .description("24-hour electronic dance music festival")
                    .venue("Beach Resort")
                    .address("789 Ocean Drive, Da Nang")
                    .eventDate(LocalDateTime.now().plusDays(45))
                    .endDate(LocalDateTime.now().plusDays(46))
                    .basePrice(new BigDecimal("800000"))
                    .totalTickets(2000)
                    .availableTickets(2000)
                    .imageUrl("https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800")
                    .status(EventStatus.ON_SALE)
                    .build();

            eventRepository.saveAll(List.of(event1, event2, event3));

            // Initialize Redis inventory for each event
            eventRepository.findAll()
                    .forEach(event -> inventoryService.initializeInventory(event.getId(), event.getAvailableTickets()));

            log.info("Sample events initialized successfully");
        }
    }

    private EventResponse toResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .description(event.getDescription())
                .venue(event.getVenue())
                .address(event.getAddress())
                .eventDate(event.getEventDate())
                .endDate(event.getEndDate())
                .basePrice(event.getBasePrice())
                .totalTickets(event.getTotalTickets())
                .availableTickets(event.getAvailableTickets())
                .imageUrl(event.getImageUrl())
                .status(event.getStatus())
                .onSale(event.isOnSale())
                .saleStartTime(event.getSaleStartTime())
                .saleEndTime(event.getSaleEndTime())
                .build();
    }
}
