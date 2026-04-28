package com.techlab.ecommerce.gateway.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.stripPrefix;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * Programmatic gateway routes. The same set could be expressed in YAML;
 * keeping it in Java keeps refactor support and lets us share constants.
 *
 * <p>JWT validation is performed by {@link com.techlab.ecommerce.gateway.security.JwtAuthFilter}
 * which runs <em>before</em> the gateway dispatches the request, so any forwarded
 * request already has {@code X-User-*} headers attached.
 */
@Configuration
@EnableConfigurationProperties(GatewayProperties.class)
public class GatewayConfig {

    @Bean
    public RouterFunction<ServerResponse> orderServiceRoute() {
        return route("order-service")
                .route(path("/api/orders/**"), http("http://localhost:8081"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> paymentServiceRoute() {
        return route("payment-service")
                .route(path("/api/payments/**"), http("http://localhost:8082"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> inventoryServiceRoute() {
        return route("inventory-service")
                .route(path("/api/inventory/**"), http("http://localhost:8083"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> notificationServiceRoute() {
        return route("notification-service")
                .route(path("/api/notifications/**"), http("http://localhost:8084"))
                .build();
    }

    @Bean
    public RouterFunction<ServerResponse> analyticsServiceRoute() {
        return route("analytics-service")
                .route(path("/api/analytics/**"), http("http://localhost:8085"))
                .build();
    }
}
