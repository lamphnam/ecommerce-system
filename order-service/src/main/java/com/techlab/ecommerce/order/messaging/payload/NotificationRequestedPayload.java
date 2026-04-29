package com.techlab.ecommerce.order.messaging.payload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload of {@code notification.requested}. Notification Service consumes this and
 * dispatches to the appropriate channel (email/push/log).
 *
 * <p>{@code type} is a free-form notification type, e.g. {@code "ORDER_CONFIRMED"},
 * {@code "ORDER_FAILED"}, {@code "PAYMENT_FAILED"}.
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
