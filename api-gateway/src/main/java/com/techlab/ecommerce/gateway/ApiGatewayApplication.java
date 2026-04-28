package com.techlab.ecommerce.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Edge entrypoint for the Techlab e-commerce platform.
 *
 * <p>Spring Cloud Gateway MVC instance. Validates incoming JWTs, strips them, and
 * forwards {@code X-User-Id}, {@code X-User-Name}, {@code X-User-Role} headers to
 * downstream services. Downstream services trust these headers (no JWT validation
 * downstream).
 */
@SpringBootApplication(scanBasePackages = {
        "com.techlab.ecommerce.gateway",
        "com.techlab.ecommerce.common"
})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
