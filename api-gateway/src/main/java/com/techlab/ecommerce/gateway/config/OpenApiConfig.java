package com.techlab.ecommerce.gateway.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger configuration for the API Gateway.
 *
 * <h3>Local gateway docs</h3>
 * The gateway's own API endpoints (e.g. {@code /api/auth/**}) are documented
 * by the default springdoc scan and appear as the "gateway" group.
 *
 * <h3>Centralized microservice docs</h3>
 * Each downstream service publishes its own {@code /v3/api-docs} endpoint.
 * The gateway proxies those through routes defined in {@link GatewayConfig}
 * (e.g. {@code /api-docs/order-service → http://localhost:8081/v3/api-docs}).
 * The {@code springdoc.swagger-ui.urls} list in {@code application.yml} points
 * the Swagger UI dropdown to each of these proxied paths, so the user can
 * explore every service's API from a single browser tab at
 * {@code http://localhost:8080/swagger-ui/index.html}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("API Gateway")
                .description("Techlab e-commerce API Gateway — Auth endpoints and centralized Swagger hub.")
                .version("v1"));
    }

    @Bean
    public GroupedOpenApi gatewayGroup() {
        return GroupedOpenApi.builder()
                .group("gateway")
                .packagesToScan("com.techlab.ecommerce.gateway")
                .build();
    }
}
