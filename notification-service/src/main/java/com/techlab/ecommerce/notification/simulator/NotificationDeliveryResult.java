package com.techlab.ecommerce.notification.simulator;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationDeliveryResult {

    boolean successful;
    String failureReason;
    long latencyMs;

    public static NotificationDeliveryResult success(long latencyMs) {
        return NotificationDeliveryResult.builder()
                .successful(true)
                .latencyMs(latencyMs)
                .build();
    }

    public static NotificationDeliveryResult failed(String reason, long latencyMs) {
        return NotificationDeliveryResult.builder()
                .successful(false)
                .failureReason(reason)
                .latencyMs(latencyMs)
                .build();
    }
}
