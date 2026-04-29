package com.techlab.ecommerce.payment.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound payload for payment.failed. Field names match what Order Service
 * expects in its PaymentFailedPayload DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentFailedPayload {

    private Long orderId;
    private String reason;
}
