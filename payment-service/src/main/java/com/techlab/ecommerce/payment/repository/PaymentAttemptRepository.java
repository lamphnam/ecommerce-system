package com.techlab.ecommerce.payment.repository;

import com.techlab.ecommerce.payment.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    long countByPaymentId(Long paymentId);
}
