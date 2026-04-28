package com.techlab.ecommerce.analytics.enums;

import lombok.Getter;

/**
 * Analytics Service-specific error codes. Reserved range: 6000-6999.
 */
@Getter
public enum AnalyticsErrorMessage {
    EVENT_NOT_FOUND(404, 6001, "Analytics event not found"),
    INVALID_TIME_RANGE(400, 6002, "Invalid time range"),
    UNSUPPORTED_AGGREGATION(400, 6003, "Unsupported aggregation");

    private final int httpStatus;
    private final int code;
    private final String message;

    AnalyticsErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
