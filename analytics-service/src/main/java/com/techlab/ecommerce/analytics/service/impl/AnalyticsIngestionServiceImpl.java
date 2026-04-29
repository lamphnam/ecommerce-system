package com.techlab.ecommerce.analytics.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.techlab.ecommerce.analytics.entity.AnalyticsEvent;
import com.techlab.ecommerce.analytics.repository.AnalyticsEventRepository;
import com.techlab.ecommerce.analytics.service.AnalyticsIngestionService;
import com.techlab.ecommerce.analytics.service.IdempotentEventProcessor;
import com.techlab.ecommerce.common.messaging.constants.QueueNames;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generic fan-out event ingester. It stores envelope metadata plus payload JSON
 * without depending on any domain service payload classes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsIngestionServiceImpl implements AnalyticsIngestionService {

    private static final List<String> AGGREGATE_ID_FIELDS = List.of(
            "orderId", "paymentId", "productId", "notificationId");

    private final IdempotentEventProcessor idempotency;
    private final AnalyticsEventRepository analyticsEventRepository;

    @Override
    @Transactional
    public void handleEvent(EventEnvelope<Object> event, String routingKey, String exchangeName) {
        idempotency.markProcessed(event, QueueNames.ANALYTICS_EVENTS);

        AnalyticsEvent analyticsEvent = AnalyticsEvent.builder()
                .eventId(event.getEventId())
                .eventType(nonBlankOrDefault(event.getEventType(), nonBlankOrDefault(routingKey, "<unknown>")))
                .sourceService(event.getSourceService())
                .aggregateId(extractAggregateId(event.getPayload()))
                .userId(event.getUserId())
                .correlationId(event.getCorrelationId())
                .routingKey(routingKey)
                .exchangeName(exchangeName)
                .payloadJson(toPayloadJson(event.getPayload()))
                .occurredAt(event.getOccurredAt())
                .receivedAt(Instant.now())
                .build();

        AnalyticsEvent saved = analyticsEventRepository.saveAndFlush(analyticsEvent);
        log.debug("Stored analytics event id={} eventId={} type={} source={} aggregateId={}",
                saved.getId(), saved.getEventId(), saved.getEventType(), saved.getSourceService(), saved.getAggregateId());
    }

    private static String toPayloadJson(Object payload) {
        return payload == null ? null : JsonUtils.toJson(payload);
    }

    private static String extractAggregateId(Object payload) {
        Map<String, Object> payloadMap = toMap(payload);
        if (payloadMap == null || payloadMap.isEmpty()) {
            return null;
        }
        for (String field : AGGREGATE_ID_FIELDS) {
            Object value = payloadMap.get(field);
            if (value != null) {
                String asString = value.toString();
                return asString.isBlank() ? null : asString;
            }
        }
        return null;
    }

    private static Map<String, Object> toMap(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof Map<?, ?> map) {
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() != null) {
                    converted.put(entry.getKey().toString(), entry.getValue());
                }
            }
            return converted;
        }
        try {
            return JsonUtils.mapper().convertValue(payload, new TypeReference<>() {});
        } catch (IllegalArgumentException ex) {
            log.debug("Could not convert payload type {} to map for aggregate id extraction",
                    payload.getClass().getName(), ex);
            return null;
        }
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
