package com.techlab.ecommerce.analytics.mapper;

import com.techlab.ecommerce.analytics.dto.response.AnalyticsEventResponse;
import com.techlab.ecommerce.analytics.entity.AnalyticsEvent;
import com.techlab.ecommerce.common.mapper.SuperConverter;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsEventMapper extends SuperConverter<AnalyticsEventResponse, AnalyticsEvent> {

    @Override
    public AnalyticsEventResponse toDto(AnalyticsEvent entity) {
        if (entity == null) {
            return null;
        }
        return AnalyticsEventResponse.builder()
                .id(entity.getId())
                .eventId(entity.getEventId())
                .eventType(entity.getEventType())
                .sourceService(entity.getSourceService())
                .aggregateId(entity.getAggregateId())
                .userId(entity.getUserId())
                .correlationId(entity.getCorrelationId())
                .routingKey(entity.getRoutingKey())
                .exchangeName(entity.getExchangeName())
                .payloadJson(entity.getPayloadJson())
                .occurredAt(entity.getOccurredAt())
                .receivedAt(entity.getReceivedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public AnalyticsEvent toEntity(AnalyticsEventResponse dto) {
        throw new UnsupportedOperationException(
                "AnalyticsEventResponse → AnalyticsEvent is not supported; analytics events are built from observed envelopes");
    }
}
