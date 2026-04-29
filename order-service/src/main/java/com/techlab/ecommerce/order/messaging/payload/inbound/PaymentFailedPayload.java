package com.techlab.ecommerce.order.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound payload for {@code payment.failed} — sent by Payment Service when a charge
 * is rejected or exhausts retries.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedPayload {

    private Long orderId;
    private String reason;
}
