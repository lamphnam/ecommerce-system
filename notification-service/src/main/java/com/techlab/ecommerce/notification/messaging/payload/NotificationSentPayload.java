package com.techlab.ecommerce.notification.messaging.payload;

import com.techlab.ecommerce.notification.enums.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outbound payload for notification.sent. Analytics will consume by JSON field names.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSentPayload {

    private Long notificationId;
    private Long userId;
    private Long orderId;
    private NotificationChannel channel;
    private String type;
    private String recipient;
}
