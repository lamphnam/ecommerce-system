package com.techlab.ecommerce.analytics.service;

import com.techlab.ecommerce.analytics.dto.response.OrderEventsSummaryResponse;
import com.techlab.ecommerce.analytics.entity.AnalyticsEvent;
import com.techlab.ecommerce.analytics.exception.DuplicateProcessedEventException;
import com.techlab.ecommerce.analytics.repository.AnalyticsEventRepository;
import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AnalyticsServiceImplTest {

    @Autowired
    private AnalyticsIngestionService ingestionService;

    @Autowired
    private AnalyticsQueryService queryService;

    @Autowired
    private AnalyticsEventRepository analyticsEventRepository;

    @Test
    void handleEvent_persistsGenericEnvelopeAndPayloadJson() {
        EventEnvelope<Object> event = event(RoutingKeys.ORDER_CREATED, "order-service",
                payload("orderId", 1001L, "totalAmount", "75.00"));

        ingestionService.handleEvent(event, RoutingKeys.ORDER_CREATED, ExchangeNames.ORDER);

        AnalyticsEvent stored = analyticsEventRepository.findAll().stream()
                .filter(candidate -> candidate.getEventId().equals(event.getEventId()))
                .findFirst()
                .orElseThrow();
        assertThat(stored.getEventType()).isEqualTo(RoutingKeys.ORDER_CREATED);
        assertThat(stored.getSourceService()).isEqualTo("order-service");
        assertThat(stored.getAggregateId()).isEqualTo("1001");
        assertThat(stored.getUserId()).isEqualTo(42L);
        assertThat(stored.getCorrelationId()).isEqualTo(event.getCorrelationId());
        assertThat(stored.getRoutingKey()).isEqualTo(RoutingKeys.ORDER_CREATED);
        assertThat(stored.getExchangeName()).isEqualTo(ExchangeNames.ORDER);
        assertThat(stored.getPayloadJson()).contains("\"orderId\":1001");
        assertThat(stored.getReceivedAt()).isNotNull();
    }

    @Test
    void handleEvent_duplicateEventIdIsSkippedBySpecificException() {
        EventEnvelope<Object> event = event(RoutingKeys.PAYMENT_SUCCEEDED, "payment-service",
                payload("orderId", 1002L, "paymentId", 501L));

        ingestionService.handleEvent(event, RoutingKeys.PAYMENT_SUCCEEDED, ExchangeNames.PAYMENT);

        assertThatThrownBy(() -> ingestionService.handleEvent(event, RoutingKeys.PAYMENT_SUCCEEDED, ExchangeNames.PAYMENT))
                .isInstanceOf(DuplicateProcessedEventException.class);

        long matchingRows = analyticsEventRepository.findAll().stream()
                .filter(candidate -> candidate.getEventId().equals(event.getEventId()))
                .count();
        assertThat(matchingRows).isEqualTo(1L);
    }

    @Test
    void orderSummary_countsObservedSagaEvents() {
        OrderEventsSummaryResponse before = queryService.orderSummary();

        ingestionService.handleEvent(event(RoutingKeys.ORDER_CREATED, "order-service",
                payload("orderId", 2001L)), RoutingKeys.ORDER_CREATED, ExchangeNames.ORDER);
        ingestionService.handleEvent(event(RoutingKeys.PAYMENT_SUCCEEDED, "payment-service",
                payload("orderId", 2001L, "paymentId", 9001L)), RoutingKeys.PAYMENT_SUCCEEDED, ExchangeNames.PAYMENT);
        ingestionService.handleEvent(event(RoutingKeys.PAYMENT_FAILED, "payment-service",
                payload("orderId", 2002L, "reason", "declined")), RoutingKeys.PAYMENT_FAILED, ExchangeNames.PAYMENT);
        ingestionService.handleEvent(event(RoutingKeys.INVENTORY_FAILED, "inventory-service",
                payload("orderId", 2003L, "reason", "out of stock")), RoutingKeys.INVENTORY_FAILED, ExchangeNames.INVENTORY);
        ingestionService.handleEvent(event(RoutingKeys.NOTIFICATION_FAILED, "notification-service",
                payload("notificationId", 3001L, "orderId", 2004L)), RoutingKeys.NOTIFICATION_FAILED, ExchangeNames.NOTIFICATION);

        OrderEventsSummaryResponse after = queryService.orderSummary();

        assertThat(after.getOrderCreatedCount()).isEqualTo(before.getOrderCreatedCount() + 1);
        assertThat(after.getPaymentSucceededCount()).isEqualTo(before.getPaymentSucceededCount() + 1);
        assertThat(after.getPaymentFailedCount()).isEqualTo(before.getPaymentFailedCount() + 1);
        assertThat(after.getInventoryFailedCount()).isEqualTo(before.getInventoryFailedCount() + 1);
        assertThat(after.getNotificationFailedCount()).isEqualTo(before.getNotificationFailedCount() + 1);
    }

    @Test
    void listEvents_filtersByAggregateId() {
        EventEnvelope<Object> event = event(RoutingKeys.NOTIFICATION_SENT, "notification-service",
                payload("notificationId", 7777L, "orderId", 3001L));
        ingestionService.handleEvent(event, RoutingKeys.NOTIFICATION_SENT, ExchangeNames.NOTIFICATION);

        var page = queryService.listEvents(null, null, "3001", null, null, null, PageRequest.of(0, 10));

        assertThat(page.getContent())
                .anySatisfy(response -> {
                    assertThat(response.getEventId()).isEqualTo(event.getEventId());
                    assertThat(response.getAggregateId()).isEqualTo("3001");
                });
    }

    private static EventEnvelope<Object> event(String eventType, String sourceService, Object payload) {
        return EventEnvelope.<Object>builder()
                .eventId("evt-" + UUID.randomUUID())
                .eventType(eventType)
                .sourceService(sourceService)
                .occurredAt(Instant.now())
                .version(1)
                .correlationId("corr-" + UUID.randomUUID())
                .userId(42L)
                .payload(payload)
                .build();
    }

    private static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            payload.put(keyValues[i].toString(), keyValues[i + 1]);
        }
        return payload;
    }
}
