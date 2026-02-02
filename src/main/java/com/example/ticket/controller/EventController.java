package com.example.ticket.controller;

import com.example.ticket.dto.response.ApiResponse;
import com.example.ticket.dto.response.EventResponse;
import com.example.ticket.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for event-related operations.
 */
@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    /**
     * Get all upcoming events.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<EventResponse>>> getUpcomingEvents() {
        List<EventResponse> events = eventService.getUpcomingEvents();
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * Get events with pagination.
     */
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> getEventsPage(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponse> events = eventService.getEvents(pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * Search events by keyword.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<EventResponse>>> searchEvents(
            @RequestParam String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<EventResponse> events = eventService.searchEvents(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(events));
    }

    /**
     * Get event by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEventById(@PathVariable Long id) {
        try {
            EventResponse event = eventService.getEventById(id);
            return ResponseEntity.ok(ApiResponse.success(event));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
