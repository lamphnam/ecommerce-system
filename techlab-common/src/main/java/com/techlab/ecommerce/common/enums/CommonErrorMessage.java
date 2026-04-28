package com.techlab.ecommerce.common.enums;

import lombok.Getter;

/**
 * Cross-cutting error codes shared by every service.
 * Service-specific codes (e.g. ORDER_NOT_FOUND, PAYMENT_DECLINED) live in the
 * service module under its own `enums` package.
 *
 * Code ranges (convention):
 *   1000-1099 generic / cross-cutting (here)
 *   2000-2999 order-service
 *   3000-3999 payment-service
 *   4000-4999 inventory-service
 *   5000-5999 notification-service
 *   6000-6999 analytics-service
 *   7000-7999 api-gateway
 *   9999      internal / unknown
 */
@Getter
public enum CommonErrorMessage {
    BAD_REQUEST(400, 1000, "Bad request"),
    VALIDATION_FAILED(400, 1001, "Validation failed"),
    UNAUTHORIZED(401, 1002, "Unauthorized"),
    FORBIDDEN(403, 1003, "Forbidden"),
    NOT_FOUND(404, 1004, "Resource not found"),
    CONFLICT(409, 1005, "Conflict"),
    UNSUPPORTED_MEDIA_TYPE(415, 1006, "Unsupported media type"),
    TOO_MANY_REQUESTS(429, 1007, "Too many requests"),
    INTERNAL_ERROR(500, 9999, "Internal server error"),
    SERVICE_UNAVAILABLE(503, 1008, "Service temporarily unavailable"),

    INVALID_TOKEN(401, 1010, "Invalid or expired token"),
    MISSING_USER_CONTEXT(401, 1011, "Missing authenticated user context"),
    IDEMPOTENCY_KEY_REQUIRED(400, 1012, "Idempotency-Key header is required"),
    DUPLICATE_EVENT(409, 1013, "Duplicate event already processed");

    private final int httpStatus;
    private final int code;
    private final String message;

    CommonErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
