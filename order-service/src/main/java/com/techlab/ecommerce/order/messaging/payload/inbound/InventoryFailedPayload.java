package com.techlab.ecommerce.order.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound payload for {@code inventory.failed} — sent by Inventory Service when stock
 * could not be reserved for an order (out of stock, product missing, etc.).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedPayload {

    private Long orderId;
    private String reason;
}
