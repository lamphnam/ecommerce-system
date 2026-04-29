package com.techlab.ecommerce.payment.repository;

import com.techlab.ecommerce.payment.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = "attempts")
    Optional<Payment> findWithAttemptsById(Long id);

    @EntityGraph(attributePaths = "attempts")
    Optional<Payment> findWithAttemptsByOrderId(Long orderId);

    Optional<Payment> findByOrderId(Long orderId);

    Page<Payment> findByOrderId(Long orderId, Pageable pageable);
}
