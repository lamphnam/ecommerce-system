package com.techlab.ecommerce.order.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload of {@code order.created}. Inventory Service consumes this to reserve stock.
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
