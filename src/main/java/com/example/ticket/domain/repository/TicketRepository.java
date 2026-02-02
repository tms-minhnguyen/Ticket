package com.example.ticket.domain.repository;

import com.example.ticket.domain.entity.Ticket;
import com.example.ticket.domain.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

// @Repository
// public interface TicketRepository extends JpaRepository<Ticket, Long> {
public interface TicketRepository { // Placeholder to avoid compilation errors in other files if imported, though
                                    // imports should be unused.
        // If Ticket entity is disabled, this repo cannot exist as a JpaRepository.
        // Ideally we comment out the whole file or make it a dummy.

        /**
         * Find available tickets for an event.
         */
        // List<Ticket> findByEventIdAndStatus(Long eventId, TicketStatus status);

        /**
         * Find tickets by order ID.
         */
        // List<Ticket> findByOrderId(Long orderId);

        /**
         * Find ticket by code.
         */
        Optional<Ticket> findByTicketCode(String ticketCode);

        /**
         * Count available tickets for an event.
         */
        // long countByEventIdAndStatus(Long eventId, TicketStatus status);

        /**
         * Update ticket status and assign to order.
         */
        // @Modifying
        // @Query("UPDATE Ticket t SET t.status = :status, t.order.id = :orderId " +
        // "WHERE t.id IN :ticketIds AND t.status = 'AVAILABLE'")
        // int holdTickets(@Param("ticketIds") List<Long> ticketIds,
        // @Param("orderId") Long orderId,
        // @Param("status") TicketStatus status);

        /**
         * Release held tickets back to available.
         */
        // @Modifying
        // @Query("UPDATE Ticket t SET t.status = 'AVAILABLE', t.order = null, t.owner =
        // null " +
        // "WHERE t.order.id = :orderId")
        // int releaseTicketsByOrderId(@Param("orderId") Long orderId);

        /**
         * Assign tickets to user.
         */
        // @Modifying
        // @Query("UPDATE Ticket t SET t.status = 'SOLD', t.owner.id = :userId " +
        // "WHERE t.order.id = :orderId")
        // int assignTicketsToUser(@Param("orderId") Long orderId, @Param("userId") Long
        // userId);
}
