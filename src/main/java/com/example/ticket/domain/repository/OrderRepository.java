package com.example.ticket.domain.repository;

import com.example.ticket.domain.entity.Order;
import com.example.ticket.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Find order by order code.
     */
    Optional<Order> findByOrderCode(String orderCode);

    /**
     * Find orders by user ID with pagination.
     */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find expired pending orders.
     */
    @Query("SELECT o FROM Order o WHERE o.status = :status AND o.expiredAt < :now")
    List<Order> findExpiredOrders(@Param("status") OrderStatus status, @Param("now") LocalDateTime now);

    /**
     * Find orders by status.
     */
    List<Order> findByStatus(OrderStatus status);

    /**
     * Update order status.
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.orderCode = :orderCode")
    int updateStatusByOrderCode(@Param("orderCode") String orderCode, @Param("status") OrderStatus status);

    /**
     * Update order status to PAID with timestamp.
     */
    @Modifying
    @Query("UPDATE Order o SET o.status = 'PAID', o.paidAt = :paidAt WHERE o.orderCode = :orderCode")
    int markAsPaid(@Param("orderCode") String orderCode, @Param("paidAt") LocalDateTime paidAt);

    /**
     * Count total tickets purchased by user for a specific event.
     */
    @Query("SELECT COALESCE(SUM(o.quantity), 0) FROM Order o WHERE o.user.id = :userId AND o.event.id = :eventId AND o.status != 'FAILED'")
    int countTicketsByUserAndEvent(@Param("userId") Long userId, @Param("eventId") Long eventId);
}
