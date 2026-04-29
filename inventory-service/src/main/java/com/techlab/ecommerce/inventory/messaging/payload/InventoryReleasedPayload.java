package com.techlab.ecommerce.inventory.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Outbound payload for inventory.released. Currently observed by analytics only;
 * included for compensation visibility and future consumers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryReleasedPayload {

    private Long orderId;
    private List<Long> reservationIds;
    private String reason;
}
