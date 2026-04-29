package com.techlab.ecommerce.payment.simulator;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentProviderResult {

    boolean successful;
    String providerReference;
    String failureReason;
    long latencyMs;

    public static PaymentProviderResult success(String providerReference, long latencyMs) {
        return PaymentProviderResult.builder()
                .successful(true)
                .providerReference(providerReference)
                .latencyMs(latencyMs)
                .build();
    }

    public static PaymentProviderResult declined(String reason, long latencyMs) {
        return PaymentProviderResult.builder()
                .successful(false)
                .failureReason(reason)
                .latencyMs(latencyMs)
                .build();
    }
}
