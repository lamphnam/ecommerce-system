package com.techlab.ecommerce.payment.service;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.enums.PaymentAttemptStatus;
import com.techlab.ecommerce.payment.enums.PaymentStatus;
import com.techlab.ecommerce.payment.exception.DuplicateProcessedEventException;
import com.techlab.ecommerce.payment.messaging.payload.PaymentFailedPayload;
import com.techlab.ecommerce.payment.messaging.payload.PaymentSucceededPayload;
import com.techlab.ecommerce.payment.messaging.payload.inbound.PaymentRequestedPayload;
import com.techlab.ecommerce.payment.repository.PaymentRepository;
import com.techlab.ecommerce.payment.simulator.PaymentProviderResult;
import com.techlab.ecommerce.payment.simulator.PaymentProviderSimulator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class PaymentServiceImplTest {

    private static final long USER_ID = 42L;

    @Autowired
    private PaymentSagaService sagaService;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private RabbitTemplate rabbitTemplate;

    @MockitoBean
    private PaymentProviderSimulator simulator;

    @AfterEach
    void resetMocks() {
        reset(rabbitTemplate, simulator);
    }

    @Test
    void handlePaymentRequested_success_createsPaymentAndPublishesPaymentSucceeded() {
        long orderId = uniqueOrderId();
        when(simulator.process(any(Payment.class), anyInt()))
                .thenReturn(PaymentProviderResult.success("PAY-TEST-123", 25L));

        sagaService.handlePaymentRequested(paymentRequestedEvent(orderId, "corr-success"));

        Payment payment = paymentRepository.findWithAttemptsByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getProviderReference()).isEqualTo("PAY-TEST-123");
        assertThat(payment.getFailureReason()).isNull();
        assertThat(payment.getAttempts()).hasSize(1);
        assertThat(payment.getAttempts().get(0).getStatus()).isEqualTo(PaymentAttemptStatus.SUCCEEDED);
        assertThat(payment.getAttempts().get(0).getProviderLatencyMs()).isEqualTo(25L);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ExchangeNames.PAYMENT), eq(RoutingKeys.PAYMENT_SUCCEEDED), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(RoutingKeys.PAYMENT_SUCCEEDED);
        assertThat(sent.getSourceService()).isEqualTo("payment-service");
        assertThat(sent.getCorrelationId()).isEqualTo("corr-success");
        assertThat(sent.getUserId()).isEqualTo(USER_ID);
        assertThat(sent.getPayload()).isInstanceOf(PaymentSucceededPayload.class);
        PaymentSucceededPayload payload = (PaymentSucceededPayload) sent.getPayload();
        assertThat(payload.getOrderId()).isEqualTo(orderId);
        assertThat(payload.getPaymentId()).isEqualTo(payment.getId());
        assertThat(payload.getAmount()).isEqualByComparingTo("99.99");
        assertThat(payload.getCurrency()).isEqualTo("USD");
    }

    @Test
    void handlePaymentRequested_declined_createsFailedPaymentAndPublishesPaymentFailed() {
        long orderId = uniqueOrderId();
        when(simulator.process(any(Payment.class), anyInt()))
                .thenReturn(PaymentProviderResult.declined("Card declined", 30L));

        sagaService.handlePaymentRequested(paymentRequestedEvent(orderId, "corr-failed"));

        Payment payment = paymentRepository.findWithAttemptsByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("Card declined");
        assertThat(payment.getAttempts()).hasSize(1);
        assertThat(payment.getAttempts().get(0).getStatus()).isEqualTo(PaymentAttemptStatus.FAILED);
        assertThat(payment.getAttempts().get(0).getProviderLatencyMs()).isEqualTo(30L);

        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate).convertAndSend(
                eq(ExchangeNames.PAYMENT), eq(RoutingKeys.PAYMENT_FAILED), captor.capture());
        EventEnvelope<?> sent = captor.getValue();
        assertThat(sent.getEventType()).isEqualTo(RoutingKeys.PAYMENT_FAILED);
        assertThat(sent.getCorrelationId()).isEqualTo("corr-failed");
        assertThat(sent.getPayload()).isInstanceOf(PaymentFailedPayload.class);
        PaymentFailedPayload payload = (PaymentFailedPayload) sent.getPayload();
        assertThat(payload.getOrderId()).isEqualTo(orderId);
        assertThat(payload.getReason()).isEqualTo("Card declined");
    }

    @Test
    void handlePaymentRequested_duplicateEventDoesNotChargeOrPublishTwice() {
        long orderId = uniqueOrderId();
        EventEnvelope<PaymentRequestedPayload> event = paymentRequestedEvent(orderId, "corr-duplicate");
        when(simulator.process(any(Payment.class), anyInt()))
                .thenReturn(PaymentProviderResult.success("PAY-DUPLICATE", 10L));

        sagaService.handlePaymentRequested(event);

        assertThatThrownBy(() -> sagaService.handlePaymentRequested(event))
                .isInstanceOf(DuplicateProcessedEventException.class);

        Payment payment = paymentRepository.findWithAttemptsByOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(payment.getAttempts()).hasSize(1);
        verify(simulator, times(1)).process(any(Payment.class), anyInt());
        ArgumentCaptor<EventEnvelope<?>> captor = ArgumentCaptor.forClass(EventEnvelope.class);
        verify(rabbitTemplate, times(1)).convertAndSend(
                eq(ExchangeNames.PAYMENT), eq(RoutingKeys.PAYMENT_SUCCEEDED), captor.capture());
    }

    private static EventEnvelope<PaymentRequestedPayload> paymentRequestedEvent(Long orderId, String correlationId) {
        PaymentRequestedPayload payload = PaymentRequestedPayload.builder()
                .orderId(orderId)
                .userId(USER_ID)
                .amount(new BigDecimal("99.99"))
                .currency("USD")
                .build();
        return EventEnvelope.<PaymentRequestedPayload>builder()
                .eventId("evt-" + UUID.randomUUID())
                .eventType(RoutingKeys.PAYMENT_REQUESTED)
                .sourceService("order-service")
                .occurredAt(Instant.now())
                .version(1)
                .correlationId(correlationId)
                .userId(USER_ID)
                .payload(payload)
                .build();
    }

    private static long uniqueOrderId() {
        return Math.abs(UUID.randomUUID().getMostSignificantBits());
    }
}
