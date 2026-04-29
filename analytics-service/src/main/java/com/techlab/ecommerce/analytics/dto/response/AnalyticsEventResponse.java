package com.techlab.ecommerce.analytics.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsEventResponse {

    private Long id;
    private String eventId;
    private String eventType;
    private String sourceService;
    private String aggregateId;
    private Long userId;
    private String correlationId;
    private String routingKey;
    private String exchangeName;
    private String payloadJson;
    private Instant occurredAt;
    private Instant receivedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
