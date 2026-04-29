package com.techlab.ecommerce.inventory.messaging.publisher;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.inventory.messaging.EventEnvelopeFactory;
import com.techlab.ecommerce.inventory.messaging.payload.InventoryFailedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.InventoryReleasedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.InventoryReservedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typed publisher for Inventory Service domain events. It publishes only to
 * inventory.exchange; analytics-service observes inventory.exchange via wildcard
 * binding, so no duplicate analytics.event message is emitted.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventEnvelopeFactory envelopeFactory;

    public void publishInventoryReserved(EventEnvelope<?> inbound, Long orderId, Long reservationId) {
        InventoryReservedPayload payload = InventoryReservedPayload.builder()
                .orderId(orderId)
                .reservationId(reservationId)
                .build();
        EventEnvelope<InventoryReservedPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.INVENTORY_RESERVED, payload, inbound);
        send(RoutingKeys.INVENTORY_RESERVED, envelope);
    }

    public void publishInventoryFailed(EventEnvelope<?> inbound, Long orderId, String reason) {
        InventoryFailedPayload payload = InventoryFailedPayload.builder()
                .orderId(orderId)
                .reason(reason)
                .build();
        EventEnvelope<InventoryFailedPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.INVENTORY_FAILED, payload, inbound);
        send(RoutingKeys.INVENTORY_FAILED, envelope);
    }

    public void publishInventoryReleased(EventEnvelope<?> inbound,
                                         Long orderId,
                                         List<Long> reservationIds,
                                         String reason) {
        InventoryReleasedPayload payload = InventoryReleasedPayload.builder()
                .orderId(orderId)
                .reservationIds(reservationIds)
                .reason(reason)
                .build();
        EventEnvelope<InventoryReleasedPayload> envelope =
                envelopeFactory.buildFromInbound(RoutingKeys.INVENTORY_RELEASED, payload, inbound);
        send(RoutingKeys.INVENTORY_RELEASED, envelope);
    }

    private <T> void send(String routingKey, EventEnvelope<T> envelope) {
        log.debug("Publishing {} (eventId={}, correlationId={}) → {}",
                envelope.getEventType(), envelope.getEventId(), envelope.getCorrelationId(), ExchangeNames.INVENTORY);
        rabbitTemplate.convertAndSend(ExchangeNames.INVENTORY, routingKey, envelope);
    }
}
