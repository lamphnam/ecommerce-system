package com.techlab.ecommerce.order.service;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.order.messaging.payload.inbound.InventoryFailedPayload;
import com.techlab.ecommerce.order.messaging.payload.inbound.InventoryReservedPayload;
import com.techlab.ecommerce.order.messaging.payload.inbound.PaymentFailedPayload;
import com.techlab.ecommerce.order.messaging.payload.inbound.PaymentSucceededPayload;

/**
 * Saga reactions to events from Inventory and Payment services. Each handler:
 *
 * <ol>
 *   <li>marks the inbound event id as processed (idempotency);</li>
 *   <li>updates the order entity to the next saga state;</li>
 *   <li>publishes the next outbound event(s) for the saga.</li>
 * </ol>
 *
 * <p>Implementations run inside a single transaction so DB updates and the
 * idempotency marker rollback together if any step fails.
 */
public interface OrderSagaService {

    void handleInventoryReserved(EventEnvelope<InventoryReservedPayload> event);

    void handleInventoryFailed(EventEnvelope<InventoryFailedPayload> event);

    void handlePaymentSucceeded(EventEnvelope<PaymentSucceededPayload> event);

    void handlePaymentFailed(EventEnvelope<PaymentFailedPayload> event);
}
