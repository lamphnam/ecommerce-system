package com.techlab.ecommerce.common.messaging.constants;

/**
 * Routing keys used to publish events. Format: {@code <domain>.<verb>}.
 * These also serve as the {@code eventType} field of {@link com.techlab.ecommerce.common.messaging.envelope.EventEnvelope}.
 */
public final class RoutingKeys {

    private RoutingKeys() {}

    // Order
    public static final String ORDER_CREATED = "order.created";
    public static final String ORDER_CONFIRMED = "order.confirmed";
    public static final String ORDER_FAILED = "order.failed";
    public static final String ORDER_CANCELLED = "order.cancelled";

    // Payment
    public static final String PAYMENT_REQUESTED = "payment.requested";
    public static final String PAYMENT_SUCCEEDED = "payment.succeeded";
    public static final String PAYMENT_FAILED = "payment.failed";

    // Inventory
    public static final String INVENTORY_RESERVE_REQUESTED = "inventory.reserve.requested";
    public static final String INVENTORY_RESERVED = "inventory.reserved";
    public static final String INVENTORY_FAILED = "inventory.failed";
    public static final String INVENTORY_RELEASE_REQUESTED = "inventory.release.requested";
    public static final String INVENTORY_RELEASED = "inventory.released";

    // Notification
    public static final String NOTIFICATION_REQUESTED = "notification.requested";
    public static final String NOTIFICATION_SENT = "notification.sent";
    public static final String NOTIFICATION_FAILED = "notification.failed";

    // Analytics fan-out (wildcard binding handled by analytics-service)
    public static final String ANALYTICS_EVENT = "analytics.event";

    // DLQ suffix convention
    public static final String DLQ_SUFFIX = ".dlq";
}
