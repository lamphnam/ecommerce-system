package com.techlab.ecommerce.order.service.impl;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.order.entity.Order;
import com.techlab.ecommerce.order.enums.OrderErrorMessage;
import com.techlab.ecommerce.order.enums.OrderStatus;
import com.techlab.ecommerce.order.exception.OrderException;
import com.techlab.ecommerce.order.messaging.payload.inbound.InventoryFailedPayload;
import com.techlab.ecommerce.order.messaging.payload.inbound.InventoryReservedPayload;
import com.techlab.ecommerce.order.messaging.payload.inbound.PaymentFailedPayload;
import com.techlab.ecommerce.order.messaging.payload.inbound.PaymentSucceededPayload;
import com.techlab.ecommerce.order.messaging.publisher.OrderEventPublisher;
import com.techlab.ecommerce.order.repository.OrderRepository;
import com.techlab.ecommerce.order.service.IdempotentEventProcessor;
import com.techlab.ecommerce.order.service.OrderSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Saga reactions. Each handler runs in a single transaction so the idempotency
 * marker, the order update, and the next-event publish either all commit or all
 * roll back together.
 *
 * <p>Phase-5 simplification: events are published with {@code RabbitTemplate}
 * inside the transaction. With publisher confirms enabled, a delivery failure
 * raises an {@code AmqpException} which rolls the DB transaction back; the
 * listener then nacks the inbound message and the broker re-delivers it (or
 * routes it to the DLQ on subsequent failures). A transactional outbox would
 * give true exactly-once semantics — listed as a future improvement in the
 * Phase 13 report.
 *
 * <h3>Out-of-order / late events</h3>
 * The saga is linear (PENDING → INVENTORY_RESERVED → CONFIRMED, with FAILED as
 * an absorbing state). Events that arrive against an order already in a
 * terminal or unexpected state are logged and skipped after the idempotency
 * marker is written, so a stale redelivery won't accidentally roll the order
 * back into an earlier state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaServiceImpl implements OrderSagaService {

    private final OrderRepository orderRepository;
    private final IdempotentEventProcessor idempotency;
    private final OrderEventPublisher publisher;

    @Override
    @Transactional
    public void handleInventoryReserved(EventEnvelope<InventoryReservedPayload> event) {
        idempotency.markProcessed(event, "order.inventory-reserved.q");

        Long orderId = event.getPayload().getOrderId();
        Order order = loadOrder(orderId, "inventory.reserved");

        if (order.getStatus() != OrderStatus.PENDING) {
            log.warn("Ignoring inventory.reserved for order {} in state {} (eventId={})",
                    orderId, order.getStatus(), event.getEventId());
            return;
        }

        order.setStatus(OrderStatus.INVENTORY_RESERVED);
        orderRepository.saveAndFlush(order);

        publisher.publishPaymentRequested(order);
        log.info("Order {} → INVENTORY_RESERVED, published payment.requested", orderId);
    }

    @Override
    @Transactional
    public void handleInventoryFailed(EventEnvelope<InventoryFailedPayload> event) {
        idempotency.markProcessed(event, "order.inventory-failed.q");

        Long orderId = event.getPayload().getOrderId();
        Order order = loadOrder(orderId, "inventory.failed");

        if (isTerminal(order.getStatus())) {
            log.warn("Ignoring inventory.failed for order {} in terminal state {} (eventId={})",
                    orderId, order.getStatus(), event.getEventId());
            return;
        }

        String reason = nonBlankOrDefault(event.getPayload().getReason(), "Inventory reservation failed");
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.saveAndFlush(order);

        publisher.publishNotificationRequested(order, "ORDER_FAILED",
                "Order #" + order.getId() + " failed: " + reason);
        log.info("Order {} → FAILED (inventory): {}", orderId, reason);
    }

    @Override
    @Transactional
    public void handlePaymentSucceeded(EventEnvelope<PaymentSucceededPayload> event) {
        idempotency.markProcessed(event, "order.payment-succeeded.q");

        Long orderId = event.getPayload().getOrderId();
        Order order = loadOrder(orderId, "payment.succeeded");

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            log.warn("Order {} already CONFIRMED, skipping payment.succeeded (eventId={})",
                    orderId, event.getEventId());
            return;
        }
        if (order.getStatus() != OrderStatus.INVENTORY_RESERVED) {
            log.warn("Ignoring payment.succeeded for order {} in unexpected state {} (eventId={})",
                    orderId, order.getStatus(), event.getEventId());
            return;
        }

        order.setStatus(OrderStatus.CONFIRMED);
        orderRepository.saveAndFlush(order);

        publisher.publishNotificationRequested(order, "ORDER_CONFIRMED",
                "Order #" + order.getId() + " confirmed.");
        log.info("Order {} → CONFIRMED, published notification.requested", orderId);
    }

    @Override
    @Transactional
    public void handlePaymentFailed(EventEnvelope<PaymentFailedPayload> event) {
        idempotency.markProcessed(event, "order.payment-failed.q");

        Long orderId = event.getPayload().getOrderId();
        Order order = loadOrder(orderId, "payment.failed");

        if (isTerminal(order.getStatus())) {
            log.warn("Ignoring payment.failed for order {} in terminal state {} (eventId={})",
                    orderId, order.getStatus(), event.getEventId());
            return;
        }

        String reason = nonBlankOrDefault(event.getPayload().getReason(), "Payment rejected");
        order.setStatus(OrderStatus.FAILED);
        order.setFailureReason(reason);
        orderRepository.saveAndFlush(order);

        // Compensation: release inventory that was previously reserved.
        publisher.publishInventoryReleaseRequested(order, "Payment failed: " + reason);
        publisher.publishNotificationRequested(order, "PAYMENT_FAILED",
                "Payment failed for order #" + order.getId() + ": " + reason);
        log.info("Order {} → FAILED (payment), published inventory.release.requested", orderId);
    }

    private Order loadOrder(Long orderId, String eventLabel) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderException(OrderErrorMessage.ORDER_NOT_FOUND,
                        "Order " + orderId + " not found while handling " + eventLabel));
    }

    private static boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.CONFIRMED
                || status == OrderStatus.FAILED
                || status == OrderStatus.CANCELLED;
    }

    private static String nonBlankOrDefault(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
