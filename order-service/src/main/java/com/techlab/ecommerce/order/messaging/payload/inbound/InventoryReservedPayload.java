package com.techlab.ecommerce.order.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound payload for {@code inventory.reserved} — sent by Inventory Service after
 * successfully reserving stock for an order.
 *
 * <p>Field names are the cross-service contract; deserialization uses
 * {@link com.techlab.ecommerce.common.messaging.config.RabbitMqCommonConfig}
 * with inferred type precedence so this DTO does not need to live in the
 * producer's package.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReservedPayload {

    private Long orderId;
    private Long reservationId;
}
