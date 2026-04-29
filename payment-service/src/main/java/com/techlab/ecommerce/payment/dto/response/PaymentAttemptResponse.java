package com.techlab.ecommerce.payment.dto.response;

import com.techlab.ecommerce.payment.enums.PaymentAttemptStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentAttemptResponse {

    private Long id;
    private Integer attemptNumber;
    private PaymentAttemptStatus status;
    private String errorMessage;
    private Long providerLatencyMs;
    private Instant attemptedAt;
}
