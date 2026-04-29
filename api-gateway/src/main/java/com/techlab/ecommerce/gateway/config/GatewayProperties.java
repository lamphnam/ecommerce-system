package com.techlab.ecommerce.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Bound to {@code gateway.*} in application.yml. Lists public path prefixes that
 * skip JWT validation, plus the JWT secret used to validate inbound tokens.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    /** Path prefixes that bypass JWT validation. */
    private List<String> publicPaths = List.of(
            "/api/auth/",
            "/v3/api-docs",
            "/swagger-ui",
            "/swagger-ui.html",
            "/actuator/health",
            "/actuator/info"
    );

    private Jwt jwt = new Jwt();

    private Services services = new Services();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        /** Token lifetime in seconds for demo tokens issued by the gateway. */
        private long expirationSeconds = 3600;
        /** Allowed clock skew in seconds when validating expiry. */
        private long clockSkewSeconds = 30;
        private String userIdClaim = "uid";
        private String usernameClaim = "sub";
        private String roleClaim = "role";
    }

    /**
     * Downstream service base URLs. Defaults target localhost dev ports so a plain
     * {@code mvnw spring-boot:run} works out of the box. Override per-environment
     * via {@code gateway.services.*-url} or the matching env vars
     * ({@code ORDER_SERVICE_URL}, etc.) — Docker Compose / Kubernetes will set
     * these to the in-cluster service addresses.
     */
    @Getter
    @Setter
    public static class Services {
        private String orderUrl = "http://localhost:8081";
        private String paymentUrl = "http://localhost:8082";
        private String inventoryUrl = "http://localhost:8083";
        private String notificationUrl = "http://localhost:8084";
        private String analyticsUrl = "http://localhost:8085";
    }
}
