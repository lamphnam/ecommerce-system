package com.techlab.ecommerce.inventory.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound payload for inventory.release.requested. Published by Order Service as
 * compensation after payment failure.
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
