package com.techlab.ecommerce.payment.dto.response;

import com.techlab.ecommerce.payment.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {

    private Long id;
    private Long orderId;
    private PaymentStatus status;
    private BigDecimal amount;
    private String currency;
    private String paymentMethod;
    private String providerReference;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;
    private List<PaymentAttemptResponse> attempts;
}
