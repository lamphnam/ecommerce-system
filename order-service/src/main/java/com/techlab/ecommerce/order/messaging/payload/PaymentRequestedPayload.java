package com.techlab.ecommerce.order.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Payload of {@code payment.requested}. Payment Service consumes this to charge the user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestedPayload {

    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private String currency;
}
