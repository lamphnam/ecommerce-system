package com.techlab.ecommerce.common.messaging.envelope;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Standard envelope for every business event published over RabbitMQ.
 * The {@code payload} type is service-specific (e.g. OrderCreatedPayload).
 *
 * Required fields:
 *   - eventId      stable per-event UUID, used by consumers as the idempotency key
 *   - eventType    routing key style name, e.g. "order.created"
 *   - sourceService originating microservice, e.g. "order-service"
 *   - occurredAt   timestamp the producer captured the event
 *   - version      payload schema version, default 1
 *   - correlationId end-to-end request id (X-Request-Id) for tracing
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventEnvelope<T> {

    private String eventId;
    private String eventType;
    private String sourceService;
    private Instant occurredAt;
    private Integer version;
    private String correlationId;
    private Long userId;
    private T payload;

    public static <T> EventEnvelope<T> of(String eventType, String sourceService, T payload) {
        return EventEnvelope.<T>builder()
                .eventId(UUID.randomUUID().toString())
                .eventType(eventType)
                .sourceService(sourceService)
                .occurredAt(Instant.now())
                .version(1)
                .payload(payload)
                .build();
    }
}
