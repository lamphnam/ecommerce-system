package com.techlab.ecommerce.payment.service.impl;

import com.techlab.ecommerce.payment.dto.request.ProcessPaymentRequest;
import com.techlab.ecommerce.payment.dto.response.PaymentResponse;
import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.entity.PaymentAttempt;
import com.techlab.ecommerce.payment.enums.PaymentAttemptStatus;
import com.techlab.ecommerce.payment.enums.PaymentErrorMessage;
import com.techlab.ecommerce.payment.enums.PaymentStatus;
import com.techlab.ecommerce.payment.exception.PaymentException;
import com.techlab.ecommerce.payment.mapper.PaymentMapper;
import com.techlab.ecommerce.payment.repository.PaymentAttemptRepository;
import com.techlab.ecommerce.payment.repository.PaymentRepository;
import com.techlab.ecommerce.payment.service.PaymentSyncService;
import com.techlab.ecommerce.payment.simulator.PaymentProviderResult;
import com.techlab.ecommerce.payment.simulator.PaymentProviderSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Synchronous payment processing for the experiment baseline.
 *
 * <p>Reuses the same core logic as {@link PaymentSagaServiceImpl}:
 * create-or-load payment → validate amount → call simulator → mark result.
 * The only difference: no EventEnvelope, no idempotency marker, no RabbitMQ publishing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncServiceImpl implements PaymentSyncService {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_PAYMENT_METHOD = "SIMULATED_CARD";

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentProviderSimulator simulator;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        Long orderId = request.getOrderId();
        Payment payment = loadOrCreatePayment(request);

        if (isTerminal(payment.getStatus())) {
            log.warn("Sync: ignoring processPayment for order {} because payment {} is already {}",
                    orderId, payment.getId(), payment.getStatus());
            return paymentMapper.toDto(payment);
        }

        int attemptNumber = nextAttemptNumber(payment);
        PaymentAttempt attempt = PaymentAttempt.builder()
                .attemptNumber(attemptNumber)
                .status(PaymentAttemptStatus.PROCESSING)
                .attemptedAt(Instant.now())
                .build();
        payment.addAttempt(attempt);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment = paymentRepository.saveAndFlush(payment);
        attempt = latestAttempt(payment, attemptNumber);

        PaymentProviderResult result = simulator.process(payment, attemptNumber);
        if (result.isSuccessful()) {
            payment.setStatus(PaymentStatus.SUCCEEDED);
            payment.setProviderReference(result.getProviderReference());
            payment.setFailureReason(null);
            attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
            attempt.setProviderLatencyMs(result.getLatencyMs());
            attempt.setErrorMessage(null);
            paymentRepository.saveAndFlush(payment);
            log.info("Sync: payment {} for order {} SUCCEEDED providerRef={}",
                    payment.getId(), orderId, payment.getProviderReference());
        } else {
            String reason = nonBlank(result.getFailureReason(), "Payment declined by simulated provider");
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(reason);
            attempt.setStatus(PaymentAttemptStatus.FAILED);
            attempt.setErrorMessage(reason);
            attempt.setProviderLatencyMs(result.getLatencyMs());
            paymentRepository.saveAndFlush(payment);
            log.info("Sync: payment {} for order {} FAILED: {}", payment.getId(), orderId, reason);
            throw new PaymentException(PaymentErrorMessage.PAYMENT_DECLINED, reason);
        }
        return paymentMapper.toDto(payment);
    }

    private Payment loadOrCreatePayment(ProcessPaymentRequest request) {
        return paymentRepository.findByOrderId(request.getOrderId())
                .orElseGet(() -> {
                    Payment created = Payment.builder()
                            .orderId(request.getOrderId())
                            .status(PaymentStatus.REQUESTED)
                            .amount(request.getAmount() == null ? BigDecimal.ZERO : request.getAmount())
                            .currency(nonBlank(request.getCurrency(), DEFAULT_CURRENCY))
                            .paymentMethod(DEFAULT_PAYMENT_METHOD)
                            .build();
                    return paymentRepository.saveAndFlush(created);
                });
    }

    private int nextAttemptNumber(Payment payment) {
        if (payment.getId() == null) return 1;
        return Math.toIntExact(attemptRepository.countByPaymentId(payment.getId()) + 1L);
    }

    private static PaymentAttempt latestAttempt(Payment payment, int attemptNumber) {
        return payment.getAttempts().stream()
                .filter(a -> a.getAttemptNumber().equals(attemptNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Payment attempt " + attemptNumber + " not persisted for payment " + payment.getId()));
    }

    private static boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }

    private static String nonBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
