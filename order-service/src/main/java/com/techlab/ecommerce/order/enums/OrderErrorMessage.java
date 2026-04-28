package com.techlab.ecommerce.order.enums;

import lombok.Getter;

/**
 * Order Service-specific error codes. Reserved range: 2000-2999.
 * Cross-cutting codes live in {@link com.techlab.ecommerce.common.enums.CommonErrorMessage}.
 */
@Getter
public enum OrderErrorMessage {
    ORDER_NOT_FOUND(404, 2001, "Order not found"),
    ORDER_ALREADY_CANCELLED(409, 2002, "Order is already cancelled"),
    ORDER_INVALID_STATUS_TRANSITION(409, 2003, "Invalid order status transition"),
    ORDER_ITEMS_REQUIRED(400, 2004, "Order must contain at least one item"),
    ORDER_DUPLICATE_IDEMPOTENCY_KEY(409, 2005, "Duplicate idempotency key");

    private final int httpStatus;
    private final int code;
    private final String message;

    OrderErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
