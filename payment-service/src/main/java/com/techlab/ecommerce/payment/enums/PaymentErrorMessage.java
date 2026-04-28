package com.techlab.ecommerce.payment.enums;

import lombok.Getter;

/**
 * Payment Service-specific error codes. Reserved range: 3000-3999.
 */
@Getter
public enum PaymentErrorMessage {
    PAYMENT_NOT_FOUND(404, 3001, "Payment not found"),
    PAYMENT_ALREADY_PROCESSED(409, 3002, "Payment already processed"),
    PAYMENT_DECLINED(402, 3003, "Payment declined by provider"),
    PAYMENT_PROVIDER_UNAVAILABLE(503, 3004, "Payment provider unavailable"),
    INVALID_PAYMENT_AMOUNT(400, 3005, "Invalid payment amount");

    private final int httpStatus;
    private final int code;
    private final String message;

    PaymentErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
