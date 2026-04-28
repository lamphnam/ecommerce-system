package com.techlab.ecommerce.common.messaging.constants;

/**
 * Exchange names used by the Techlab platform.
 * All business exchanges are {@code topic} unless explicitly noted otherwise.
 *
 * Each exchange has a paired DLX named {@code <name>.dlx} for poison messages,
 * declared in each consuming service's RabbitMqConfig.
 *
 * The {@link #RETRY_DELAYED} exchange uses the {@code x-delayed-message}
 * plugin to schedule delayed retries (e.g. retry-payment-after-30s).
 */
public final class ExchangeNames {

    private ExchangeNames() {}

    public static final String ORDER = "order.exchange";
    public static final String PAYMENT = "payment.exchange";
    public static final String INVENTORY = "inventory.exchange";
    public static final String NOTIFICATION = "notification.exchange";
    public static final String ANALYTICS = "analytics.exchange";

    public static final String ORDER_DLX = "order.exchange.dlx";
    public static final String PAYMENT_DLX = "payment.exchange.dlx";
    public static final String INVENTORY_DLX = "inventory.exchange.dlx";
    public static final String NOTIFICATION_DLX = "notification.exchange.dlx";
    public static final String ANALYTICS_DLX = "analytics.exchange.dlx";

    public static final String RETRY_DELAYED = "retry.exchange";
}
