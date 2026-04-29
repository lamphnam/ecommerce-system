package com.techlab.ecommerce.order.messaging;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.common.security.UserContextHolder;
import com.techlab.ecommerce.common.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Builds {@link EventEnvelope} instances with the standard fields populated from the
 * current request context:
 *
 * <ul>
 *   <li>{@code eventId}        — random UUID per event</li>
 *   <li>{@code sourceService}  — {@value #SOURCE_SERVICE}</li>
 *   <li>{@code occurredAt}     — {@code Instant.now()}</li>
 *   <li>{@code version}        — payload schema version, default 1</li>
 *   <li>{@code correlationId}  — current X-Request-Id (from {@link MDC}), or a fresh UUID
 *                                 if absent (e.g. for events emitted from saga listeners)</li>
 *   <li>{@code userId}         — from {@link UserContextHolder} when on a request thread</li>
 * </ul>
 *
 * <p>For events emitted from message-driven threads (saga listeners), the caller
 * may pass an explicit {@code userId} since {@link UserContextHolder} is empty
 * there.
 */
@Component
public class EventEnvelopeFactory {

    public static final String SOURCE_SERVICE = "order-service";
    public static final int CURRENT_VERSION = 1;

    public <T> EventEnvelope<T> build(String eventType, T payload) {
        return build(eventType, payload, UserContextHolder.currentUserId());
    }

    public <T> EventEnvelope<T> build(String eventType, T payload, Long userId) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .sourceService(SOURCE_SERVICE)
                .occurredAt(Instant.now())
                .version(CURRENT_VERSION)
                .correlationId(currentCorrelationId())
                .userId(userId)
                .payload(payload)
                .build();
    }

    private static String currentCorrelationId() {
        String fromMdc = MDC.get(CorrelationIdFilter.MDC_KEY);
        return (fromMdc != null && !fromMdc.isBlank()) ? fromMdc : UUID.randomUUID().toString();
    }
}
