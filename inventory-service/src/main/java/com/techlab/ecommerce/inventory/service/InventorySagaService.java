package com.techlab.ecommerce.inventory.service;

import com.techlab.ecommerce.common.messaging.envelope.EventEnvelope;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.InventoryReleaseRequestedPayload;
import com.techlab.ecommerce.inventory.messaging.payload.inbound.OrderCreatedPayload;

public interface InventorySagaService {

    void handleOrderCreated(EventEnvelope<OrderCreatedPayload> event);

    void handleInventoryReleaseRequested(EventEnvelope<InventoryReleaseRequestedPayload> event);
}
