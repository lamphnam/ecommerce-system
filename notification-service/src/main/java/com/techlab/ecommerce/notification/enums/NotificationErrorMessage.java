package com.techlab.ecommerce.notification.enums;

import lombok.Getter;

/**
 * Notification Service-specific error codes. Reserved range: 5000-5999.
 */
@Getter
public enum NotificationErrorMessage {
    NOTIFICATION_NOT_FOUND(404, 5001, "Notification not found"),
    UNSUPPORTED_CHANNEL(400, 5002, "Unsupported notification channel"),
    INVALID_RECIPIENT(400, 5003, "Invalid recipient"),
    TEMPLATE_NOT_FOUND(404, 5004, "Notification template not found"),
    INVALID_NOTIFICATION_EVENT(400, 5005, "Invalid notification event");

    private final int httpStatus;
    private final int code;
    private final String message;

    NotificationErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
