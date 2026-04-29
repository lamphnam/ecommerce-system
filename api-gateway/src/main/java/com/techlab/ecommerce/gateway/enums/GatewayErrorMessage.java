package com.techlab.ecommerce.gateway.enums;

import lombok.Getter;

/**
 * API Gateway-specific error codes. Reserved range: 7000-7999.
 */
@Getter
public enum GatewayErrorMessage {
    UPSTREAM_TIMEOUT(504, 7001, "Upstream service timeout"),
    UPSTREAM_UNAVAILABLE(503, 7002, "Upstream service unavailable"),
    RATE_LIMIT_EXCEEDED(429, 7003, "Rate limit exceeded"),
    ROUTE_NOT_FOUND(404, 7004, "Route not found"),
    USERNAME_ALREADY_EXISTS(409, 7005, "Username already exists"),
    INVALID_CREDENTIALS(401, 7006, "Invalid username or password"),
    USER_DISABLED(403, 7007, "User account is disabled");

    private final int httpStatus;
    private final int code;
    private final String message;

    GatewayErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
