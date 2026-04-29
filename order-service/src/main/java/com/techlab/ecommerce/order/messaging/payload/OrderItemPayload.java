package com.techlab.ecommerce.order.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Item line embedded in {@link OrderCreatedPayload} and {@link PaymentRequestedPayload}.
 * Field names are the contract — keep them stable across services.
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
