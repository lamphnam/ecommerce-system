package com.techlab.ecommerce.notification.messaging.payload.inbound;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Inbound payload for notification.requested. Field names must match Order
 * Service's outbound NotificationRequestedPayload; class/package names do not matter.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequestedPayload {

    private Long userId;
    private Long orderId;
    private String type;
    private String summary;
}
