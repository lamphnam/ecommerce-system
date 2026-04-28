package com.techlab.ecommerce.common.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authenticated user information forwarded by the API Gateway via headers.
 * Downstream services do **not** validate JWT themselves — they trust the gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserContext {
    private Long userId;
    private String username;
    private String role;
}
