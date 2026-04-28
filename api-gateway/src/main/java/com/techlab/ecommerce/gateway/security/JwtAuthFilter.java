package com.techlab.ecommerce.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.techlab.ecommerce.common.dto.ApiResponse;
import com.techlab.ecommerce.common.enums.CommonErrorMessage;
import com.techlab.ecommerce.common.security.UserContextFilter;
import com.techlab.ecommerce.common.web.CorrelationIdFilter;
import com.techlab.ecommerce.gateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Validates the JWT on inbound requests, then attaches gateway-injected
 * {@code X-User-Id}, {@code X-User-Name}, {@code X-User-Role} headers so
 * downstream services can populate
 * {@link com.techlab.ecommerce.common.security.UserContextHolder}
 * via {@link com.techlab.ecommerce.common.security.UserContextFilter}.
 *
 * <p>Public paths (configured under {@code gateway.public-paths}) bypass JWT
 * validation entirely (e.g. {@code /api/auth/**}, swagger, actuator health).
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30) // runs after CorrelationIdFilter, before gateway dispatch
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = HttpHeaders.AUTHORIZATION;
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final GatewayProperties properties;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtService jwtService, GatewayProperties properties, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPublic(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            writeUnauthorized(request, response, "Missing Authorization header");
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (RuntimeException e) {
            writeUnauthorized(request, response, "Invalid or expired token");
            return;
        }

        Map<String, String> injected = new HashMap<>();
        String userId = jwtService.extractUserId(claims);
        String username = jwtService.extractUsername(claims);
        String role = jwtService.extractRole(claims);

        if (userId != null) injected.put(UserContextFilter.HEADER_USER_ID, userId);
        if (username != null) injected.put(UserContextFilter.HEADER_USERNAME, username);
        if (role != null) injected.put(UserContextFilter.HEADER_USER_ROLE, role);

        filterChain.doFilter(new InjectedHeadersRequestWrapper(request, injected), response);
    }

    private boolean isPublic(String uri) {
        if (uri == null) return false;
        for (String prefix : properties.getPublicPaths()) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        String traceId = (String) request.getAttribute(CorrelationIdFilter.REQUEST_ID_ATTRIBUTE);
        ApiResponse<Void> body = ApiResponse.error(CommonErrorMessage.INVALID_TOKEN, traceId);
        body.getError().setMessage(message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    /**
     * Wraps the inbound request so downstream gateway dispatch sees the
     * gateway-injected headers in addition to the original headers.
     */
    private static class InjectedHeadersRequestWrapper extends HttpServletRequestWrapper {

        private final Map<String, String> injected;

        InjectedHeadersRequestWrapper(HttpServletRequest request, Map<String, String> injected) {
            super(request);
            this.injected = injected;
        }

        @Override
        public String getHeader(String name) {
            String override = injected.get(name);
            if (override != null) return override;
            // case-insensitive lookup
            for (Map.Entry<String, String> entry : injected.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(name)) return entry.getValue();
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String override = getHeader(name);
            if (override != null) return Collections.enumeration(Collections.singletonList(override));
            return super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            java.util.Set<String> names = new java.util.LinkedHashSet<>();
            Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) names.add(original.nextElement());
            names.addAll(injected.keySet());
            return Collections.enumeration(names);
        }
    }
}
