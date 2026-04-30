package com.techlab.ecommerce.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * Programmatic gateway routes. The same set could be expressed in YAML;
 * keeping it in Java keeps refactor support and lets us share constants.
 *
 * <p>Downstream URLs come from {@link GatewayProperties.Services} so they can be
 * overridden per environment (docker-compose, Kubernetes, etc.) without
 * recompiling. Defaults target the localhost dev ports.
 *
 * <p>JWT validation is performed by {@link com.techlab.ecommerce.gateway.security.JwtAuthFilter}
 * which runs <em>before</em> the gateway dispatches the request, so any forwarded
 * request already has {@code X-User-*} headers attached.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoute(GatewayProperties properties) {
        return route("order-service")
                .route(path("/api/orders/**"), http(properties.getServices().getOrderUrl()))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoute(GatewayProperties properties) {
        return route("payment-service")
                .route(path("/api/payments/**"), http(properties.getServices().getPaymentUrl()))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceRoute(GatewayProperties properties) {
        return route("inventory-service")
                .route(path("/api/inventory/**"), http(properties.getServices().getInventoryUrl()))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute(GatewayProperties properties) {
        return route("notification-service")
                .route(path("/api/notifications/**"), http(properties.getServices().getNotificationUrl()))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> analyticsServiceRoute(GatewayProperties properties) {
        return route("analytics-service")
                .route(path("/api/analytics/**"), http(properties.getServices().getAnalyticsUrl()))
                .build();
    }

    // ---- Proxied OpenAPI docs for centralized Swagger UI ----

    @Bean
    public RouterFunction<ServerResponse> orderServiceDocsRoute(GatewayProperties properties) {
        return route("order-service-docs")
                .route(path("/api-docs/order-service"), http(properties.getServices().getOrderUrl()))
                .filter(setPath("/v3/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceDocsRoute(GatewayProperties properties) {
        return route("payment-service-docs")
                .route(path("/api-docs/payment-service"), http(properties.getServices().getPaymentUrl()))
                .filter(setPath("/v3/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceDocsRoute(GatewayProperties properties) {
        return route("inventory-service-docs")
                .route(path("/api-docs/inventory-service"), http(properties.getServices().getInventoryUrl()))
                .filter(setPath("/v3/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceDocsRoute(GatewayProperties properties) {
        return route("notification-service-docs")
                .route(path("/api-docs/notification-service"), http(properties.getServices().getNotificationUrl()))
                .filter(setPath("/v3/api-docs"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> analyticsServiceDocsRoute(GatewayProperties properties) {
        return route("analytics-service-docs")
                .route(path("/api-docs/analytics-service"), http(properties.getServices().getAnalyticsUrl()))
                .filter(setPath("/v3/api-docs"))
                .build();
    }
}
