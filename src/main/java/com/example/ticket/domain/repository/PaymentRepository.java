package com.example.ticket.domain.repository;

import com.example.ticket.domain.entity.Payment;
import com.example.ticket.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Find payment by order ID.
     */
    Optional<Payment> findByOrderId(Long orderId);

    /**
     * Find payment by VNPay transaction reference.
     */
    Optional<Payment> findByVnpayTxnRef(String vnpayTxnRef);

    /**
     * Find payment by order code.
     */
    @Query("SELECT p FROM Payment p JOIN p.order o WHERE o.orderCode = :orderCode")
    Optional<Payment> findByOrderCode(@Param("orderCode") String orderCode);

    /**
     * Update payment status.
     */
    @Modifying
    @Query("UPDATE Payment p SET p.status = :status WHERE p.vnpayTxnRef = :txnRef")
    int updateStatusByTxnRef(@Param("txnRef") String txnRef, @Param("status") PaymentStatus status);
}
