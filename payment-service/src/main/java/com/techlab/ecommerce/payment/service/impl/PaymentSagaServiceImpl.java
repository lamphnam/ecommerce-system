package com.techlab.ecommerce.payment.service.impl;

import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.entity.PaymentAttempt;
import com.techlab.ecommerce.payment.enums.PaymentAttemptStatus;
import com.techlab.ecommerce.payment.enums.PaymentErrorMessage;
import com.techlab.ecommerce.payment.enums.PaymentStatus;
import com.techlab.ecommerce.payment.exception.PaymentException;
import com.techlab.ecommerce.payment.messaging.payload.inbound.PaymentRequestedPayload;
import com.techlab.ecommerce.payment.messaging.publisher.PaymentEventPublisher;
import com.techlab.ecommerce.payment.repository.PaymentAttemptRepository;
import com.techlab.ecommerce.payment.repository.PaymentRepository;
import com.techlab.ecommerce.payment.service.IdempotentEventProcessor;
import com.techlab.ecommerce.payment.service.PaymentSagaService;
import com.techlab.ecommerce.payment.simulator.PaymentProviderResult;
import com.techlab.ecommerce.payment.simulator.PaymentProviderSimulator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment side of the order saga.
 *
 * <p>Business/provider declines are not listener failures: they persist a FAILED
 * payment, publish {@code payment.failed}, and the listener acks. Unexpected
 * infrastructure/programming failures escape the transaction; the manual ack
 * wrapper nacks without requeue so RabbitMQ dead-letters the message.
 *
 * <p>Phase-7 simplification: events are published inside the transaction. A
 * transactional outbox is the production-grade next improvement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSagaServiceImpl implements PaymentSagaService {

    private static final String DEFAULT_CURRENCY = "USD";
    private static final String DEFAULT_PAYMENT_METHOD = "SIMULATED_CARD";

    private final IdempotentEventProcessor idempotency;
    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentProviderSimulator simulator;
    private final PaymentEventPublisher publisher;

    @Override
    @Transactional
    public void handlePaymentRequested(EventEnvelope<PaymentRequestedPayload> event) {
        idempotency.markProcessed(event, QueueNames.PAYMENT_PROCESS);

        PaymentRequestedPayload payload = requirePayload(event);
        Long orderId = requireOrderId(payload.getOrderId());
        Payment payment = loadOrCreatePayment(payload);

        if (isTerminal(payment.getStatus())) {
            log.warn("Ignoring payment.requested for order {} because payment {} is already {} (eventId={})",
                    orderId, payment.getId(), payment.getStatus(), event.getEventId());
            return;
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

        String validationError = validatePaymentAmount(payload.getAmount());
        if (validationError != null) {
            markFailed(payment, attempt, validationError, 0L);
            paymentRepository.saveAndFlush(payment);
            publisher.publishPaymentFailed(event, payment, validationError);
            log.info("Payment {} for order {} failed validation: {}", payment.getId(), orderId, validationError);
            return;
        }

        PaymentProviderResult result = simulator.process(payment, attemptNumber);
        if (result.isSuccessful()) {
            markSucceeded(payment, attempt, result);
            paymentRepository.saveAndFlush(payment);
            publisher.publishPaymentSucceeded(event, payment);
            log.info("Payment {} for order {} SUCCEEDED providerRef={}",
                    payment.getId(), orderId, payment.getProviderReference());
        } else {
            String reason = nonBlankOrDefault(result.getFailureReason(), "Payment declined by simulated provider");
            markFailed(payment, attempt, reason, result.getLatencyMs());
            paymentRepository.saveAndFlush(payment);
            publisher.publishPaymentFailed(event, payment, reason);
            log.info("Payment {} for order {} FAILED: {}", payment.getId(), orderId, reason);
        }
    }

    private Payment loadOrCreatePayment(PaymentRequestedPayload payload) {
        return paymentRepository.findByOrderId(payload.getOrderId())
                .orElseGet(() -> {
                    Payment created = Payment.builder()
                            .orderId(payload.getOrderId())
                            .status(PaymentStatus.REQUESTED)
                            .amount(payload.getAmount() == null ? BigDecimal.ZERO : payload.getAmount())
                            .currency(nonBlankOrDefault(payload.getCurrency(), DEFAULT_CURRENCY))
                            .paymentMethod(DEFAULT_PAYMENT_METHOD)
                            .build();
                    return paymentRepository.saveAndFlush(created);
                });
    }

    private int nextAttemptNumber(Payment payment) {
        if (payment.getId() == null) {
            return 1;
        }
        return Math.toIntExact(attemptRepository.countByPaymentId(payment.getId()) + 1L);
    }

    private static PaymentAttempt latestAttempt(Payment payment, int attemptNumber) {
        return payment.getAttempts().stream()
                .filter(candidate -> candidate.getAttemptNumber().equals(attemptNumber))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Payment attempt " + attemptNumber + " was not persisted for payment " + payment.getId()));
    }

    private static void markSucceeded(Payment payment,
                                      PaymentAttempt attempt,
                                      PaymentProviderResult result) {
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setProviderReference(result.getProviderReference());
        payment.setFailureReason(null);
        attempt.setStatus(PaymentAttemptStatus.SUCCEEDED);
        attempt.setProviderLatencyMs(result.getLatencyMs());
        attempt.setErrorMessage(null);
    }

    private static void markFailed(Payment payment,
                                   PaymentAttempt attempt,
                                   String reason,
                                   Long latencyMs) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        attempt.setStatus(PaymentAttemptStatus.FAILED);
        attempt.setErrorMessage(reason);
        attempt.setProviderLatencyMs(latencyMs);
    }

    private static PaymentRequestedPayload requirePayload(EventEnvelope<PaymentRequestedPayload> event) {
        if (event == null || event.getPayload() == null) {
            throw new PaymentException(PaymentErrorMessage.INVALID_PAYMENT_EVENT,
                    "Inbound payment event has no payload");
        }
        return event.getPayload();
    }

    private static Long requireOrderId(Long orderId) {
        if (orderId == null) {
            throw new PaymentException(PaymentErrorMessage.INVALID_PAYMENT_EVENT,
                    "payment.requested payload is missing orderId");
        }
        return orderId;
    }

    private static String validatePaymentAmount(BigDecimal amount) {
        if (amount == null) {
            return "Payment amount is missing";
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return "Payment amount must be greater than zero";
        }
        return null;
    }

    private static boolean isTerminal(PaymentStatus status) {
        return status == PaymentStatus.SUCCEEDED || status == PaymentStatus.FAILED;
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
