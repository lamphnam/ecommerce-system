package com.techlab.ecommerce.inventory.enums;

import lombok.Getter;

/**
 * Inventory Service-specific error codes. Reserved range: 4000-4999.
 */
@Getter
public enum InventoryErrorMessage {
    PRODUCT_NOT_FOUND(404, 4001, "Product not found"),
    INSUFFICIENT_STOCK(409, 4002, "Insufficient stock"),
    RESERVATION_NOT_FOUND(404, 4003, "Stock reservation not found"),
    RESERVATION_ALREADY_RELEASED(409, 4004, "Stock reservation already released"),
    INVALID_STOCK_ADJUSTMENT(400, 4005, "Invalid stock adjustment"),
    PRODUCT_SKU_ALREADY_EXISTS(409, 4006, "Product SKU already exists"),
    INVALID_INVENTORY_EVENT(400, 4007, "Invalid inventory event");

    private final int httpStatus;
    private final int code;
    private final String message;

    InventoryErrorMessage(int httpStatus, int code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
