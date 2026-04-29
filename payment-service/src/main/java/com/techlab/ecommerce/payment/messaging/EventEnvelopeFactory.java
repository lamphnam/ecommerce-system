package com.techlab.ecommerce.payment.messaging;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.common.security.UserContextHolder;
import com.techlab.ecommerce.common.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds Payment Service event envelopes. For message-driven saga events, the
 * caller passes the inbound envelope so correlationId/userId propagate across
 * Order → Payment → Order.
 */
@Component
public class EventEnvelopeFactory {

    public static final String SOURCE_SERVICE = "payment-service";
    public static final int CURRENT_VERSION = 1;

    public <T> EventEnvelope<T> build(String eventType, T payload) {
        return build(eventType, payload, currentCorrelationId(), UserContextHolder.currentUserId());
    }

    public <T> EventEnvelope<T> buildFromInbound(String eventType, T payload, EventEnvelope<?> inbound) {
        String correlationId = inbound != null && hasText(inbound.getCorrelationId())
                ? inbound.getCorrelationId()
                : currentCorrelationId();
        Long userId = inbound != null && inbound.getUserId() != null
                ? inbound.getUserId()
                : UserContextHolder.currentUserId();
        return build(eventType, payload, correlationId, userId);
    }

    private <T> EventEnvelope<T> build(String eventType, T payload, String correlationId, Long userId) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .sourceService(SOURCE_SERVICE)
                .occurredAt(Instant.now())
                .version(CURRENT_VERSION)
                .correlationId(correlationId)
                .userId(userId)
                .payload(payload)
                .build();
    }

    private static String currentCorrelationId() {
        String fromMdc = MDC.get(CorrelationIdFilter.MDC_KEY);
        return hasText(fromMdc) ? fromMdc : UUID.randomUUID().toString();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
