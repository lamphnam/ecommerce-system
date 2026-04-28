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
            "/actuator/health",
            "/actuator/info"
    );

    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        private String secret;
        /** Allowed clock skew in seconds when validating expiry. */
        private long clockSkewSeconds = 30;
        private String userIdClaim = "uid";
        private String usernameClaim = "sub";
        private String roleClaim = "role";
    }
}
