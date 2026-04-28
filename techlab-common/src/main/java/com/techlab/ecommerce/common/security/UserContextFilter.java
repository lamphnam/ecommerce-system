package com.techlab.ecommerce.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads gateway-injected headers and populates {@link UserContextHolder} for the current request.
 * Headers are written by the API Gateway after JWT validation.
 *
 * Headers:
 *   X-User-Id    numeric user id (required for authenticated endpoints)
 *   X-User-Name  username (optional)
 *   X-User-Role  role string (e.g. USER, ADMIN)
 *
 * Downstream services do **not** validate JWT; they trust the gateway boundary.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class UserContextFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_ID = "X-User-Id";
    public static final String HEADER_USERNAME = "X-User-Name";
    public static final String HEADER_USER_ROLE = "X-User-Role";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String userIdHeader = request.getHeader(HEADER_USER_ID);
            if (userIdHeader != null && !userIdHeader.isBlank()) {
                Long userId = parseLongSafe(userIdHeader);
                if (userId != null) {
                    UserContextHolder.set(UserContext.builder()
                            .userId(userId)
                            .username(request.getHeader(HEADER_USERNAME))
                            .role(request.getHeader(HEADER_USER_ROLE))
                            .build());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            UserContextHolder.clear();
        }
    }

    private static Long parseLongSafe(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
