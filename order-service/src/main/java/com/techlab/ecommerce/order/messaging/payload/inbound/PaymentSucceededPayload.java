package com.techlab.ecommerce.order.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound payload for {@code payment.succeeded} — sent by Payment Service after
 * successfully charging the user.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentSucceededPayload {

    private Long orderId;
    private Long paymentId;
    private BigDecimal amount;
    private String currency;
}
