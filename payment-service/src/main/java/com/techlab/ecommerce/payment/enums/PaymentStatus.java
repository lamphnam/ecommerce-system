package com.techlab.ecommerce.payment.enums;

/**
 * Lifecycle of a payment inside Payment Service.
 */
public enum PaymentStatus {
    REQUESTED,
    PROCESSING,
    SUCCEEDED,
    FAILED
}
