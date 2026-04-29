package com.techlab.ecommerce.inventory.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Item line embedded in order.created. Field names must match Order Service's
 * outbound payload; class/package names intentionally do not matter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemPayload {

    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}
