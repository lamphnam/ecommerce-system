package com.techlab.ecommerce.order.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload of {@code inventory.release.requested}. Compensation event published when
 * payment fails after inventory was already reserved. Inventory Service consumes it
 * to free the reserved stock.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleaseRequestedPayload {

    private Long orderId;
    private Long userId;
    private String reason;
}
