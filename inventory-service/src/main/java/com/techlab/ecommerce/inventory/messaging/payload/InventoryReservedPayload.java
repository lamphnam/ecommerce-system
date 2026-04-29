package com.techlab.ecommerce.inventory.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound payload for inventory.reserved. Field names match what Order Service
 * expects in its InventoryReservedPayload DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedPayload {

    private Long orderId;
    private Long reservationId;
}
