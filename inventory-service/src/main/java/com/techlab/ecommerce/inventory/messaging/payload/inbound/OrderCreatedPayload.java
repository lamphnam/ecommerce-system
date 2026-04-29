package com.techlab.ecommerce.inventory.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Inbound payload for order.created. Inventory Service consumes this to reserve
 * product stock for the order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreatedPayload {

    private Long orderId;
    private Long userId;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemPayload> items;
}
