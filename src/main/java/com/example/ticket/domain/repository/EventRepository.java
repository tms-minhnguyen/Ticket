package com.example.ticket.domain.repository;

import com.example.ticket.domain.entity.Event;
import com.example.ticket.domain.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * Find all events that are on sale and haven't happened yet.
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.eventDate > :now ORDER BY e.eventDate ASC")
    List<Event> findUpcomingEventsByStatus(@Param("status") EventStatus status, @Param("now") LocalDateTime now);

    /**
     * Find events on sale with pagination.
     */
    Page<Event> findByStatusAndEventDateAfterOrderByEventDateAsc(
            EventStatus status, LocalDateTime now, Pageable pageable);

    /**
     * Search events by name or venue.
     */
    @Query("SELECT e FROM Event e WHERE e.status = :status AND e.eventDate > :now " +
            "AND (LOWER(e.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(e.venue) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Event> searchEvents(
            @Param("status") EventStatus status,
            @Param("now") LocalDateTime now,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * Decrement available tickets atomically.
     */
    @Modifying
    @Query("UPDATE Event e SET e.availableTickets = e.availableTickets - :quantity " +
            "WHERE e.id = :eventId AND e.availableTickets >= :quantity")
    int decrementAvailableTickets(@Param("eventId") Long eventId, @Param("quantity") int quantity);

    /**
     * Increment available tickets (for release/refund).
     */
    @Modifying
    @Query("UPDATE Event e SET e.availableTickets = e.availableTickets + :quantity " +
            "WHERE e.id = :eventId")
    int incrementAvailableTickets(@Param("eventId") Long eventId, @Param("quantity") int quantity);
}
