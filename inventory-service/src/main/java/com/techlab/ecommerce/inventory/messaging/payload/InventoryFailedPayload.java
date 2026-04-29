package com.techlab.ecommerce.inventory.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound payload for inventory.failed. Field names match what Order Service
 * expects in its InventoryFailedPayload DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryFailedPayload {

    private Long orderId;
    private String reason;
}
