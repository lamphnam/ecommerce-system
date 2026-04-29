package com.techlab.ecommerce.payment.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Inbound payload for payment.requested. Field names must match Order Service's
 * outbound PaymentRequestedPayload; class/package names intentionally do not matter.
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
