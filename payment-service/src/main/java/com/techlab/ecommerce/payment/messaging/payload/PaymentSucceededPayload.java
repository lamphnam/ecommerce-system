package com.techlab.ecommerce.payment.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Outbound payload for payment.succeeded. Field names match what Order Service
 * expects in its PaymentSucceededPayload DTO.
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
