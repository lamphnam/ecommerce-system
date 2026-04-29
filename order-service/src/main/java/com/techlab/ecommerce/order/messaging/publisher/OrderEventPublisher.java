package com.techlab.ecommerce.order.messaging.publisher;

import com.techlab.ecommerce.common.messaging.constants.ExchangeNames;
import com.techlab.ecommerce.common.messaging.constants.RoutingKeys;
import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.order.entity.Order;
import com.techlab.ecommerce.order.entity.OrderItem;
import com.techlab.ecommerce.order.mapper.OrderMapper;
import com.techlab.ecommerce.order.messaging.EventEnvelopeFactory;
import com.techlab.ecommerce.order.messaging.payload.InventoryReleaseRequestedPayload;
import com.techlab.ecommerce.order.messaging.payload.NotificationRequestedPayload;
import com.techlab.ecommerce.order.messaging.payload.OrderCreatedPayload;
import com.techlab.ecommerce.order.messaging.payload.OrderItemPayload;
import com.techlab.ecommerce.order.messaging.payload.PaymentRequestedPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Typed publisher for the four domain events Order Service emits. Every method
 * builds an {@link EventEnvelope} via {@link EventEnvelopeFactory} (which copies
 * correlation id from MDC and user id from UserContext), then sends to the
 * matching topic exchange.
 *
 * <p>Phase-5 simplification: publishes directly inside the same DB transaction.
 * Using a transactional outbox would give true exactly-once semantics — that's
 * called out as a future improvement in the saga service Javadoc and in the
 * Phase 13 report.
 *
 * <p>Order Service does <strong>not</strong> publish to {@code analytics.exchange}.
 * Analytics observes the domain exchanges via wildcard bindings — see
 * {@link ExchangeNames} for the strategy.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final EventEnvelopeFactory envelopeFactory;
    private final OrderMapper orderMapper;

    public void publishOrderCreated(Order order) {
        List<OrderItemPayload> itemPayloads = order.getItems().stream()
                .map(orderMapper::toItemPayload)
                .toList();
        OrderCreatedPayload payload = OrderCreatedPayload.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
                .items(itemPayloads)
                .build();
        EventEnvelope<OrderCreatedPayload> envelope =
                envelopeFactory.build(RoutingKeys.ORDER_CREATED, payload, order.getUserId());
        send(ExchangeNames.ORDER, RoutingKeys.ORDER_CREATED, envelope);
    }

    public void publishPaymentRequested(Order order) {
        PaymentRequestedPayload payload = PaymentRequestedPayload.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .amount(order.getTotalAmount())
                .currency(order.getCurrency())
                .build();
        EventEnvelope<PaymentRequestedPayload> envelope =
                envelopeFactory.build(RoutingKeys.PAYMENT_REQUESTED, payload, order.getUserId());
        send(ExchangeNames.PAYMENT, RoutingKeys.PAYMENT_REQUESTED, envelope);
    }

    public void publishInventoryReleaseRequested(Order order, String reason) {
        InventoryReleaseRequestedPayload payload = InventoryReleaseRequestedPayload.builder()
                .orderId(order.getId())
                .userId(order.getUserId())
                .reason(reason)
                .build();
        EventEnvelope<InventoryReleaseRequestedPayload> envelope = envelopeFactory.build(
                RoutingKeys.INVENTORY_RELEASE_REQUESTED, payload, order.getUserId());
        send(ExchangeNames.INVENTORY, RoutingKeys.INVENTORY_RELEASE_REQUESTED, envelope);
    }

    public void publishNotificationRequested(Order order, String type, String summary) {
        NotificationRequestedPayload payload = NotificationRequestedPayload.builder()
                .userId(order.getUserId())
                .orderId(order.getId())
                .type(type)
                .summary(summary)
                .build();
        EventEnvelope<NotificationRequestedPayload> envelope = envelopeFactory.build(
                RoutingKeys.NOTIFICATION_REQUESTED, payload, order.getUserId());
        send(ExchangeNames.NOTIFICATION, RoutingKeys.NOTIFICATION_REQUESTED, envelope);
    }

    private <T> void send(String exchange, String routingKey, EventEnvelope<T> envelope) {
        log.debug("Publishing {} (eventId={}, correlationId={}) → {}",
                envelope.getEventType(), envelope.getEventId(), envelope.getCorrelationId(), exchange);
        rabbitTemplate.convertAndSend(exchange, routingKey, envelope);
    }
}
