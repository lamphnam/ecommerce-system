package com.techlab.ecommerce.common.messaging.constants;

/**
 * Queue names. Convention: {@code <consumer-service>.<purpose>.q} for primary queues
 * and {@code <consumer-service>.<purpose>.dlq} for the dead-letter sibling.
 *
 * Each queue is declared in the **consuming** service's RabbitMqConfig with:
 *   - durable=true
 *   - x-dead-letter-exchange = matching *.dlx
 *   - x-dead-letter-routing-key = original routing key + ".dlq"
 */
public final class QueueNames {

    private QueueNames() {}

    // Inventory consumes order.created
    public static final String INVENTORY_RESERVE = "inventory.reserve.q";
    public static final String INVENTORY_RESERVE_DLQ = "inventory.reserve.dlq";

    // Inventory consumes inventory.release.requested (compensation)
    public static final String INVENTORY_RELEASE = "inventory.release.q";
    public static final String INVENTORY_RELEASE_DLQ = "inventory.release.dlq";

    // Order consumes inventory.reserved / inventory.failed
    public static final String ORDER_INVENTORY_RESERVED = "order.inventory-reserved.q";
    public static final String ORDER_INVENTORY_RESERVED_DLQ = "order.inventory-reserved.dlq";
    public static final String ORDER_INVENTORY_FAILED = "order.inventory-failed.q";
    public static final String ORDER_INVENTORY_FAILED_DLQ = "order.inventory-failed.dlq";

    // Payment consumes payment.requested
    public static final String PAYMENT_PROCESS = "payment.process.q";
    public static final String PAYMENT_PROCESS_DLQ = "payment.process.dlq";

    // Order consumes payment.succeeded / payment.failed
    public static final String ORDER_PAYMENT_SUCCEEDED = "order.payment-succeeded.q";
    public static final String ORDER_PAYMENT_SUCCEEDED_DLQ = "order.payment-succeeded.dlq";
    public static final String ORDER_PAYMENT_FAILED = "order.payment-failed.q";
    public static final String ORDER_PAYMENT_FAILED_DLQ = "order.payment-failed.dlq";

    // Notification consumes notification.requested
    public static final String NOTIFICATION_SEND = "notification.send.q";
    public static final String NOTIFICATION_SEND_DLQ = "notification.send.dlq";

    // Analytics fan-out (wildcard)
    public static final String ANALYTICS_EVENTS = "analytics.events.q";
    public static final String ANALYTICS_EVENTS_DLQ = "analytics.events.dlq";
}
