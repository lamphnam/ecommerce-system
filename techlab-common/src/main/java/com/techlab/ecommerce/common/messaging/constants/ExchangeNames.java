package com.techlab.ecommerce.common.messaging.constants;

/**
 * Exchange names used by the Techlab platform.
 * All business exchanges are {@code topic} unless explicitly noted otherwise.
 *
 * <p>Each exchange has a paired DLX named {@code <name>.dlx} for poison messages,
 * declared in each consuming service's RabbitMqConfig.
 *
 * <h2>Analytics strategy</h2>
 * Analytics is collected by binding {@code analytics.events.q} (owned by
 * analytics-service) to every <em>domain</em> exchange — {@link #ORDER},
 * {@link #PAYMENT}, {@link #INVENTORY}, {@link #NOTIFICATION} — with a
 * {@code #} wildcard. Each service therefore publishes only its normal
 * domain events; <strong>nothing should publish a duplicate
 * {@code analytics.event} message</strong> to {@link #ANALYTICS} for the same
 * fact, otherwise the event is recorded twice.
 *
 * <p>{@link #ANALYTICS} is reserved for future analytics-internal events
 * (e.g. re-emitted aggregates, replays). Today it is owned by
 * analytics-service and bound into {@code analytics.events.q} so that any such
 * future event still flows in without topology changes.
 *
 * <p>The {@link #RETRY_DELAYED} exchange uses the {@code x-delayed-message}
 * plugin to schedule delayed retries (e.g. retry-payment-after-30s).
 */
public final class ExchangeNames {

    private ExchangeNames() {}

    public static final String ORDER = "order.exchange";
    public static final String PAYMENT = "payment.exchange";
    public static final String INVENTORY = "inventory.exchange";
    public static final String NOTIFICATION = "notification.exchange";

    /**
     * Reserved for future analytics-internal events. Do <strong>not</strong>
     * publish here for the domain events emitted by order/payment/inventory/
     * notification — those are already captured by analytics-service via
     * wildcard bindings on the corresponding domain exchanges.
     */
    public static final String ANALYTICS = "analytics.exchange";

    public static final String ORDER_DLX = "order.exchange.dlx";
    public static final String PAYMENT_DLX = "payment.exchange.dlx";
    public static final String INVENTORY_DLX = "inventory.exchange.dlx";
    public static final String NOTIFICATION_DLX = "notification.exchange.dlx";
    public static final String ANALYTICS_DLX = "analytics.exchange.dlx";

    public static final String RETRY_DELAYED = "retry.exchange";
}
