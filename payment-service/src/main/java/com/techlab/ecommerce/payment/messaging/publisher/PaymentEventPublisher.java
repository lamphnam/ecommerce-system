package com.techlab.ecommerce.payment.messaging.publisher;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.payment.entity.Payment;
import com.techlab.ecommerce.payment.messaging.EventEnvelopeFactory;
import com.techlab.ecommerce.payment.messaging.payload.PaymentFailedPayload;
import com.techlab.ecommerce.payment.messaging.payload.PaymentSucceededPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Typed publisher for Payment Service domain events. It publishes only to
 * payment.exchange; analytics-service observes payment.exchange via wildcard
 * binding, so no duplicate analytics.event message is emitted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventEnvelopeFactory envelopeFactory;

    public void publishPaymentSucceeded(EventEnvelope<?> inbound, Payment payment) {
        PaymentSucceededPayload payload = PaymentSucceededPayload.builder()
                .orderId(payment.getOrderId())
                .paymentId(payment.getId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .build();
        EventEnvelope<PaymentSucceededPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.PAYMENT_SUCCEEDED, payload, inbound);
        send(RoutingKeys.PAYMENT_SUCCEEDED, envelope);
    }

    public void publishPaymentFailed(EventEnvelope<?> inbound, Payment payment, String reason) {
        PaymentFailedPayload payload = PaymentFailedPayload.builder()
                .orderId(payment.getOrderId())
                .reason(reason)
                .build();
        EventEnvelope<PaymentFailedPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.PAYMENT_FAILED, payload, inbound);
        send(RoutingKeys.PAYMENT_FAILED, envelope);
    }

    private <T> void send(String routingKey, EventEnvelope<T> envelope) {
        log.debug("Publishing {} (eventId={}, correlationId={}) → {}",
                envelope.getEventType(), envelope.getEventId(), envelope.getCorrelationId(), ExchangeNames.PAYMENT);
        rabbitTemplate.convertAndSend(ExchangeNames.PAYMENT, routingKey, envelope);
    }
}
